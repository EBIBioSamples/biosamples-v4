#!/bin/bash
set -e
mvn -T 2C clean package

docker-compose down -v --remove-orphans

#docker volume ls -q | xargs -r docker volume rm
#docker images -q | xargs -r docker rmi

docker-compose build
docker-compose up -d biosamples-webapps-api

./http-status-check -u http://localhost:8081/

java -jar integration/target/integration-4.0.0-SNAPSHOT.jar --phase1

docker-compose up -d biosamples-agents-neo4j biosamples-agents-solr

echo "sleeping for 180 seconds..."
sleep 180

java -jar integration/target/integration-4.0.0-SNAPSHOT.jar --phase2

