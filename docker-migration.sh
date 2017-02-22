#!/bin/bash
set -e
mvn -T 2C package

docker-compose down -v --remove-orphans

#docker volume ls -q | xargs -r docker volume rm
#docker images -q | xargs -r docker rmi

docker-compose build
docker-compose up -d biosamples-webapps-api biosamples-agents-neo4j biosamples-agents-solr
#docker-compose scale biosamples-agents-neo4j=5 biosamples-agents-solr=5

./http-status-check -u http://localhost:8081/ -t 300

time java -jar pipelines/accession/target/pipelines-accession-4.0.0-SNAPSHOT.jar --spring.datasource.accession.url=jdbc:oracle:thin:@ora-vm-063.ebi.ac.uk:1541:biosddev --spring.datasource.accession.username=bsd_acc --spring.datasource.accession.password=b5d4ccpr0 --spring.datasource.accession.driver-class-name=oracle.jdbc.driver.OracleDriver

time java -jar pipelines/ena/target/pipelines-ena-4.0.0-SNAPSHOT.jar --spring.datasource.erapro.url=jdbc:oracle:thin:@ora-vm-009.ebi.ac.uk:1541:ERAPRO --spring.datasource.erapro.username=era_reader --spring.datasource.erapro.password=reader --spring.datasource.erapro.driver-class-name=oracle.jdbc.driver.OracleDriver

wget -O /tmp/biosample_set.xml.gz http://ftp.ncbi.nih.gov/biosample/biosample_set.xml.gz 
time java -jar pipelines/ncbi/target/pipelines-ncbi-4.0.0-SNAPSHOT.jar --biosamples.ncbi.file=/tmp/biosample_set.xml.gz