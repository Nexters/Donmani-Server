FROM openjdk:17-ea-11-jdk-slim
COPY build/libs/donmani_server-0.0.1-SNAPSHOT.jar donmani_server.jar
ENTRYPOINT ["java", "-jar", "/donmani_server.jar", "--spring.profiles.active=prod"]
