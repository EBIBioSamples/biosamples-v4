#use alpine base for minimal size
#includes OpenJDK 8 (and Maven 3)
FROM openjdk:8-jre-alpine

MAINTAINER EBI BioSamples <biosamples@ebi.ac.uk>

COPY webapps/*/target/*.war agents/*/target/*.jar pipelines/target/*.jar /