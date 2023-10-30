#!/bin/bash
set -e

./docker-webapp.sh --clean


docker-compose up -d biosamples-agents-solr
docker-compose up -d biosamples-agents-upload-workers

#ARGS=--spring.profiles.active=big
for X in 1 2 3 4 5 6
do
  echo "============================================================================================================"
  echo "=================================== STARTING INTEGRATION TESTS PHASE-"$X "====================================="
  echo "============================================================================================================"
  #java -jar integration/target/integration-4.0.0-SNAPSHOT.jar --phase=$X $ARGS $@
  docker-compose run --rm --service-ports biosamples-integration java -jar integration-5.2.21-SNAPSHOT.jar --phase=$X $ARGS $@
  sleep 10 #solr is configured to commit every 5 seconds

done

#leave the agent up at the end
docker-compose up -d biosamples-agents-solr

echo "Successfully completed"
