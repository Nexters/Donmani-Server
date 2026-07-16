FROM openjdk:17-ea-11-jdk-slim
ENV TZ=Asia/Seoul
COPY build/libs/donmani_server-0.0.1-SNAPSHOT.jar donmani_server.jar
ENTRYPOINT ["java", "-Duser.timezone=Asia/Seoul", "-jar", "/donmani_server.jar", "--spring.profiles.active=prod"]
