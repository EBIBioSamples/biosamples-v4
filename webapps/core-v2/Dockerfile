#
# Build stage
#
FROM maven:3.6.0-jdk-11-slim AS build
COPY src /home/app/src
COPY pom.xml /home/app
RUN mvn -f /home/app/pom.xml clean package

FROM openjdk:8-jdk-alpine
COPY --from=build /home/app/target/*.jar /usr/local/lib/webapps-core-v2.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/usr/local/lib/webapps-core-v2.jar"]
