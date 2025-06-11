FROM eclipse-temurin:17-jdk-alpine
RUN mkdir /deployment
COPY webapps/core/target/webapps-core-*.war /deployment/webapps-core.war
RUN ls -l /deployment
ENTRYPOINT ["java", "-jar", "/deployment/webapps-core.war"]
