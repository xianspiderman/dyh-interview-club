FROM maven:3.9.9-eclipse-temurin-8 AS build
ARG SERVICE
WORKDIR /workspace
COPY . .
RUN mvn -B -ntp -pl dyh-club-runtime-services/${SERVICE} -am -DskipTests package

FROM eclipse-temurin:8-jre-alpine
ARG SERVICE
WORKDIR /app
RUN addgroup -S club && adduser -S club -G club
COPY --from=build /workspace/dyh-club-runtime-services/${SERVICE}/target/${SERVICE}-1.0-SNAPSHOT.jar app.jar
USER club
ENTRYPOINT ["java", "-XX:+ExitOnOutOfMemoryError", "-jar", "/app/app.jar"]
