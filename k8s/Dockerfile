FROM openjdk:17-jre-slim as base
WORKDIR /app

# Stage for core webapp
FROM base as webapp-core
COPY webapps/core/target/webapps-core-*.war app.war
EXPOSE 8080
CMD ["java", "-jar", "app.war"]

# Stage for core webapp
FROM base as webapp-core-v2
COPY webapps/core-v2/target/webapps-core-*.war app.war
EXPOSE 8080
CMD ["java", "-jar", "app.war"]

# Stage for agents-solr
FROM base as agents-solr
COPY agents/solr/target/agents-solr-*.jar app.jar
EXPOSE 8081
CMD ["java", "-jar", "app.jar"]
