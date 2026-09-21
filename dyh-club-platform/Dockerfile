FROM maven:3.9.9-eclipse-temurin-8 AS build
WORKDIR /workspace
COPY dyh-club-lock-spring-boot-starter/pom.xml dyh-club-lock-spring-boot-starter/pom.xml
COPY dyh-club-platform/pom.xml dyh-club-platform/pom.xml
COPY dyh-club-lock-spring-boot-starter/src dyh-club-lock-spring-boot-starter/src
COPY dyh-club-platform/src dyh-club-platform/src
RUN mvn -B -ntp -f dyh-club-lock-spring-boot-starter/pom.xml -DskipTests install \
    && mvn -B -ntp -f dyh-club-platform/pom.xml -DskipTests package
FROM eclipse-temurin:8-jre-alpine
WORKDIR /app
RUN addgroup -S club && adduser -S club -G club
COPY --from=build /workspace/dyh-club-platform/target/dyh-club-platform-1.0-SNAPSHOT.jar app.jar
USER club
EXPOSE 3020
ENTRYPOINT ["java", "-XX:+ExitOnOutOfMemoryError", "-jar", "/app/app.jar"]
