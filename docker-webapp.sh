#!/bin/bash
set -e

mvn -T 2C clean package

docker-compose stop biosamples-webapps-core biosamples-webapps-sampletab mongo neo4j solr rabbitmq biosamples-agents-neo4j biosamples-agents-solr biosamples-agents-curation
docker-compose rm -f biosamples-webapps-core biosamples-webapps-sampletab mongo neo4j solr rabbitmq biosamples-agents-neo4j biosamples-agents-solr biosamples-agents-curation

#cleanup any previous data
docker volume ls -q | grep mongo_data | xargs -r docker volume rm
docker volume ls -q | grep neo4j_data | xargs -r docker volume rm
docker volume ls -q | grep solr_samples_data | xargs -r docker volume rm
docker volume ls -q | grep rabbitmq_data | xargs -r docker volume rm
#remove any images, in case of out-of-date or corrupt images
#docker images -q | xargs -r docker rmi

#rm docker/logs/*.log docker/logs/*.log.* docker/logs/neo4j/*.log

#make sure we have up-to-date jar files in the docker image
docker-compose build


#start up the webapps (and dependencies)
docker-compose up -d biosamples-webapps-core biosamples-webapps-sampletab

echo "checking solr is up"
./http-status-check -u http://localhost:8983 -t 30
echo "checking neo4j is up"
./http-status-check -u http://localhost:7474 -t 30

echo "checking webapps-core is up"
#would like to check on /health but currently it is bugged so solr is always down
./http-status-check -u http://localhost:8081/biosamples/beta/actuator -t 30
./http-status-check -u http://localhost:8081/biosamples/beta/samples -t 30
echo "checking webapps-sampletab is up"
./http-status-check -u http://localhost:8082/biosamples/beta/health -t 30