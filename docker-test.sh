#!/bin/bash
set -e

ARGS=
ARGS="$ARGS --biosamples.client.uri=http://localhost:8081/biosamples/beta" 
ARGS="$ARGS --biosamples.submissionuri.sampletab=http://localhost:8082/biosamples/beta/sampletab"
ARGS="$ARGS --biosamples.legacyxml.uri=http://localhost:8083/biosamples/beta/xml"
#ARGS="$ARGS --spring.profiles.active=default"

for X in 1 2 3 4 5
do
  java -jar integration/target/integration-4.0.0-SNAPSHOT.jar --phase=$X $ARGS $@
  docker-compose run --rm --service-ports biosamples-agents-solr java -jar agents-solr-4.0.0-SNAPSHOT.jar --biosamples.agent.solr.stayalive=false --biosamples.agent.solr.queuetime=500
done

echo "Successfully completed"

