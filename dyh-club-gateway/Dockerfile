FROM maven:3.9.9-eclipse-temurin-8 AS build
WORKDIR /workspace
COPY dyh-club-gateway/pom.xml pom.xml
COPY dyh-club-gateway/src src
RUN mvn -B -ntp -DskipTests package
FROM eclipse-temurin:8-jre-alpine
WORKDIR /app
RUN addgroup -S club && adduser -S club -G club
COPY --from=build /workspace/target/dyh-club-gateway.jar app.jar
USER club
EXPOSE 5000
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
