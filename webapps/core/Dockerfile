FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app
COPY target/webapps-core-5.3.10-SNAPSHOT.war core.war
EXPOSE 8087
ENTRYPOINT ["java", "-jar", "core.war"]
