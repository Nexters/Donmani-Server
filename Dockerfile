FROM openjdk:17-ea-11-jdk-slim
ARG JAR_FILE=./build/libs/donmani_server-0.0.1-SNAPSHOT.jar
COPY ${JAR_FILE} donmani_server.jar
ENTRYPOINT ["java", "-jar", "/donmani_server.jar", "--spring.profiles.active=prod"]
