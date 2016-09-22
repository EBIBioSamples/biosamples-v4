#use alpine base for minimal size
#includes OpenJDK 8 and Maven 3
FROM maven:3-jdk-8-alpine

MAINTAINER EBI BioSamples <biosamples@ebi.ac.uk>


#need to add git
RUN apk add --no-cache git

#checkout from github
#build the code
#move end products away
#make them executable
#clean up
RUN git clone https://github.com/EBIBioSamples/biosamples-v4 biosamples \
    && cd biosamples \
    && mvn package \
    && mv webapps/*/target/*.war ../ \
    && mv agents/*/target/*.jar ../ \
    && mv pipelines/target/*.jar ../ \
    && chmod +x ../*.jar \
    && cd ../ \
    && rm -r biosamples

