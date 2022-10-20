package io.narsha.proxyrecorder.resources;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.logging.Log;
import io.smallrye.config.source.yaml.YamlConfigSource;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Path("/proxy")
public class ProxyResource {

    private static Map<String, String> configs = new HashMap<>();

    @Context
    UriInfo uri;

    @Context
    HttpHeaders headers;

    @Inject
    ObjectMapper objectMapper;

    @ConfigProperty(name = "folder.backup")
    String backupFolder;

    
    public ProxyResource(@ConfigProperty(name = "folder.scan") String scanFolder) throws IOException {
        System.out.println(scanFolder);
        List<File> ymls = getYmls(new File(scanFolder));

        final List<String> urls = new ArrayList<>();
        for (File f : ymls) {
            try {
                final YamlConfigSource source = new YamlConfigSource(new URL("file://" + f));
                List<String> list = source.getPropertyNames().stream()
                        .filter(e -> e.toLowerCase().contains("url"))
                        .collect(Collectors.toList());

                urls.addAll(list.stream()
                        .map(source::getValue)
                        .filter(e -> e.toLowerCase().startsWith("http"))
                        .collect(Collectors.toList()));
            }catch (Exception e) {
                Log.warn(String.format("Cannot load file : %s, cause : %s", f.getAbsolutePath(), e.getMessage()));
            }

        }
        configs = toMap(urls);
    }

    @GET
    @Path("{var:.*}")
    @Produces(MediaType.TEXT_PLAIN)
    public Object hello2(@PathParam("var") String id) throws IOException {
        MultivaluedMap<String, String> queryMap = uri.getQueryParameters();

        String[] parts = id.split("/");
        String basePath = parts[0];
        if (!configs.containsKey(basePath)) {
            System.out.println(configs);
            return null;
        }

        String endPath = Stream.of(parts).skip(1).collect(Collectors.joining("/"));
        String param = queryMap.entrySet().stream().sorted(Map.Entry.comparingByKey()).map(e -> e.getKey() + "=" + String.join(",", e.getValue())).collect(Collectors.joining("&"));

        String checkUrl = (endPath.isEmpty() ? "" : "/") + endPath + "?" + param;
        String url = configs.get(basePath) + checkUrl;

        String fileName = checkUrl + ".json";

        File f = new File(backupFolder + "/" + basePath + "/" + fileName);
        System.out.println(checkUrl);
        if (!f.exists()) {
            System.out.printf("Loading url %s%n", checkUrl);
            WebTarget target = ClientBuilder.newBuilder().build().target(url);
            Response response = target.request(MediaType.APPLICATION_JSON)
                    .get();

            String result = response.readEntity(String.class);

            Map<String, Object> headers = new HashMap<>();

            for (Map.Entry<String, List<Object>> header : response.getHeaders().entrySet()) {
                if (header.getKey().equals("Content-Type") || header.getKey().equals("Content-Length")) {
                    continue;
                }
                headers.put(header.getKey(), header.getValue());
            }

            Map<String, Object> toSave = new HashMap<>();

            toSave.put("headers", headers);
            toSave.put("status", response.getStatus());
            toSave.put("body", result);

            String data = objectMapper.writeValueAsString(toSave);
            f.getParentFile().mkdirs();
            Files.write(f.toPath(), data.getBytes(StandardCharsets.UTF_8));
        }

        String data = Files.readString(f.toPath());
        Map<String, Object> content = objectMapper.readValue(data, Map.class);

        String body = content.get("body").toString();

       Response.ResponseBuilder res = Response.status((Integer)content.get("status")).entity(body);

        for (Map.Entry<String, List<Object>> header : ((Map<String, List<Object>>)content.get("headers")).entrySet()) {
            if (header.getKey().equals("Content-Type") || header.getKey().equals("Content-Length")) {
                continue;
            }
            res.header(header.getKey(), header.getValue());
        }

        return res.build();
    }

    public static Map<String, String> toMap(List<String> list) {
        final Map<String, String> res = new HashMap<>();
        for (final String url : list) {
            final String[] parts = url.split("/");
            final String end = parts[parts.length - 1];
            res.put(end, url);
        }
        return res;
    }

    public static List<File> getYmls(File base) {
        List<File> res = new ArrayList<>();
        if (base.isDirectory()) {
            for (File file : base.listFiles()) {
                if (file.isDirectory()) {
                    res.addAll(getYmls(file));
                } else {
                    if ((file.getName().toLowerCase().endsWith(".yml") || file.getName().toLowerCase().endsWith(".yaml"))) {
                        res.add(file);
                    }
                }
            }
        }
        return res;
    }
}