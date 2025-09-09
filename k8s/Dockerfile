FROM eclipse-temurin:17-jdk-alpine as base
WORKDIR /app

FROM base as webapp-core
COPY webapps/core/target/webapps-core-*.war app.war
EXPOSE 8080
CMD ["java", "-jar", "app.war"]

FROM base as webapp-core-v2
COPY webapps/core-v2/target/webapps-core-*.war app.war
EXPOSE 8080
CMD ["java", "-jar", "app.war"]

FROM base as pipelines-reindex
COPY pipelines/reindex/target/pipelines-reindex-*.jar app.jar
EXPOSE 8081
CMD ["java", "-jar", "app.jar"]
