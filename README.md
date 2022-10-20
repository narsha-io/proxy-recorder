## Running the application 

Build the application 
```shell script
./mvnw package -Dquarkus.package.type=uber-jar -Dmaven.test.skip=true
```

Run the application using application variable
```shell script
#folder.backup if you want a fresh backup after a reboot
#set folder.backup in another folder to keep the same backup
java -Dfolder.scan="/home/<user>/config" \
     -Dfolder.backup="/tmp" \
     -jar target/proxy-recorder-<version>-runner.jar
```

Run the application using environment variable
```shell script
#folder.backup if you want a fresh backup after a reboot
#set folder.backup in another folder to keep the same backup
FOLDER_SCAN="/home/<user>/config" \
FOLDER_BACKUP="/tmp" \
java -jar target/proxy-recorder-1.0.0-SNAPSHOT-runner.jar
```


## Running the application in dev mode

You can run your application in dev mode that enables live coding using:
```shell script
./mvnw compile quarkus:dev
```

> **_NOTE:_**  Quarkus now ships with a Dev UI, which is available in dev mode only at http://localhost:8080/q/dev/.

## Packaging and running the application

The application can be packaged using:
```shell script
./mvnw package
```
It produces the `quarkus-run.jar` file in the `target/quarkus-app/` directory.
Be aware that it’s not an _über-jar_ as the dependencies are copied into the `target/quarkus-app/lib/` directory.

The application is now runnable using `java -jar target/quarkus-app/quarkus-run.jar`.

If you want to build an _über-jar_, execute the following command:
```shell script
./mvnw package -Dquarkus.package.type=uber-jar
```

The application, packaged as an _über-jar_, is now runnable using `java -jar target/*-runner.jar`.

## Creating a native executable

You can create a native executable using: 
```shell script
./mvnw package -Pnative
```

Or, if you don't have GraalVM installed, you can run the native executable build in a container using: 
```shell script
./mvnw package -Pnative -Dquarkus.native.container-build=true
```

You can then execute your native executable with: `./target/proxy-recorder-1.0.0-SNAPSHOT-runner`