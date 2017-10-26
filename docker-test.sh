#!/bin/bash
set -e

ARGS=

for X in 2 3 4 5
do
  #java -jar integration/target/integration-4.0.0-SNAPSHOT.jar --phase=$X $ARGS $@
  docker-compose run --rm --service-ports biosamples-integration java -jar integration-4.0.0-SNAPSHOT.jar --phase=$X $ARGS $@
  docker-compose run --rm --service-ports biosamples-agents-solr java -jar agents-solr-4.0.0-SNAPSHOT.jar --biosamples.agent.solr.stayalive=false --biosamples.agent.solr.queuetime=500
done

echo "Successfully completed"

