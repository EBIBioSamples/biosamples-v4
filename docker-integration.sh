#!/bin/bash
set -e
mvn -T 2C clean package

docker-compose down -v --remove-orphans

#docker volume ls -q | xargs -r docker volume rm
#docker images -q | xargs -r docker rmi

docker-compose build
docker-compose up -d biosamples-webapps-core biosamples-webapps-sampletab

./http-status-check -u http://localhost:8081/actuator -t 60
./http-status-check -u http://localhost:8082/actuator -t 60

java -jar integration/target/integration-4.0.0-SNAPSHOT.jar --phase1

docker-compose up -d biosamples-agents-neo4j biosamples-agents-solr

echo "sleeping for 30 seconds..."
sleep 30

java -jar integration/target/integration-4.0.0-SNAPSHOT.jar --phase2

