FROM eclipse-temurin:17-jdk-alpine as base
WORKDIR /app

FROM base as webapp-core
COPY webapps/core/target/webapps-core-*.war app.war
EXPOSE 8080
CMD ["java", "-jar", "app.war"]

FROM base as webapp-core-v2
COPY webapps/core-v2/target/webapps-core-*.jar app.jar
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]

FROM base as agents-uploadworkers
COPY agents/uploadworkers/target/agents-uploadworkers-*.jar app.jar
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]

FROM base as pipelines-reindex
COPY pipelines/reindex/target/pipelines-reindex-*.jar app.jar
CMD ["java", "-jar", "app.jar"]

FROM base as pipelines-curation
COPY pipelines/curation/target/pipelines-curation-*.jar app.jar
CMD ["java", "-jar", "app.jar"]

FROM base as pipelines-curami
COPY pipelines/curami/target/pipelines-curami-*.jar app.jar
CMD ["java", "-jar", "app.jar"]

FROM base as pipelines-copydown
COPY pipelines/copydown/target/pipelines-copydown-*.jar app.jar
CMD ["java", "-jar", "app.jar"]
