#!/bin/bash
set -e

docker-compose down -v --remove-orphans &

mvn -T 2C clean package

wait

docker volume ls -q | xargs -r docker volume rm
#docker images -q | xargs -r docker rmi

docker-compose build
docker-compose up -d biosamples-webapps-core biosamples-webapps-sampletab

#would like to check on /health but currently it is bugged so solr is always down
./http-status-check -u http://localhost:8081/samples -t 30
./http-status-check -u http://localhost:8082/actuator -t 30

java -jar integration/target/integration-4.0.0-SNAPSHOT.jar --phase=1

docker-compose up -d biosamples-agents-neo4j biosamples-agents-solr biosamples-agents-curation

echo "sleeping for 10 seconds..."
sleep 10

java -jar integration/target/integration-4.0.0-SNAPSHOT.jar --phase=2

