FROM maven:3.5.2-jdk-8-slim AS MAVEN_TOOL_CHAIN
ADD . /tmp/
RUN ls -la /tmp/*
WORKDIR /tmp/
RUN mvn -U -P embl-ebi clean package -Dembedmongo.wait -pl !pipelines

FROM openjdk:8-jre-alpine
MAINTAINER EBI BioSamples <biosamples@ebi.ac.uk>

COPY --from=MAVEN_TOOL_CHAIN /tmp/webapps/*/target/*.war /tmp/agents/*/target/*.jar /tmp/pipelines/*/target/*.jar /tmp/integration/target/*.jar /


