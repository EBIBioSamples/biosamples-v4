FROM openjdk:8-jre-alpine
MAINTAINER EBI BioSamples <biosamples@ebi.ac.uk>

COPY webapps/*/target/*.war webapps/*/target/*.jar agents/*/target/*.jar pipelines/*/target/*.jar integration/target/*.jar /

