#!/bin/bash
set -e

./docker-webapp.sh --clean


docker-compose up -d biosamples-agents-solr
docker-compose up -d biosamples-agents-upload-workers

ARGS=--spring.profiles.active=big
for X in 1 2 3 4 5
do
  #java -jar integration/target/integration-4.0.0-SNAPSHOT.jar --phase=$X $ARGS $@
  docker-compose run --rm --service-ports biosamples-integration java -jar integration-5.3.8-RC1.jar --phase=$X $ARGS $@
  sleep 10 #solr is configured to commit every 5 seconds

done

#leave the agents up at the end
docker-compose up -d biosamples-agents-solr
docker-compose up -d biosamples-agents-upload-workers

echo "Successfully completed"
