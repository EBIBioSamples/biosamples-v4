#!/bin/bash
set -e
mvn -T 2C package

docker-compose down -v --remove-orphans

#docker volume ls -q | xargs -r docker volume rm
#docker images -q | xargs -r docker rmi

docker-compose build
docker-compose up -d biosamples-webapps-api

./http-status-check -u http://localhost:8081/

java -jar integration/target/integration-4.0.0-SNAPSHOT.jar