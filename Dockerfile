FROM openjdk:11-jre
MAINTAINER EBI BioSamples <biosamples@ebi.ac.uk>

COPY webapps/*/target/*.war agents/*/target/*.jar pipelines/*/target/*.jar integration/target/*.jar /

