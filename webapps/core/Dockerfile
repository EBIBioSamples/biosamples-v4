FROM eclipse-temurin:17-jdk-alpine
RUN mkdir /deployment
COPY webapps-core-*.war /deployment/webapps-core.war
ENTRYPOINT ["java", "-jar", "/deployment/webapps-core.war"]
