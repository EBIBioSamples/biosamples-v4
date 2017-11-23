#!/bin/bash
set -e

./docker-webapp.sh --clean

#to pass along username/password set them as environment variables like so:
#export biosamples_client_aap_username=
#export biosamples_client_aap_password=
#export biosamples_legacyapikey=

#setup arguments to use for tests
ARGS=
#ARGS="$ARGS --biosamples.client.uri=http://localhost:8081/biosamples/beta" 
#ARGS="$ARGS --biosamples.submissionuri.sampletab=http://localhost:8082/biosamples/beta/sampletab"
#ARGS="$ARGS --biosamples.legacyxml.uri=http://localhost:8083/biosamples/beta/xml"
#ARGS="$ARGS --spring.profiles.active=default"

for X in 1 2 3 4 5
do
  #java -jar integration/target/integration-4.0.0-RC1.jar --phase=$X $ARGS $@
  docker-compose run --rm --service-ports biosamples-integration java -jar integration-4.0.0-RC1.jar --phase=$X $ARGS $@
  docker-compose run --rm --service-ports biosamples-agents-solr java -jar agents-solr-4.0.0-RC1.jar --biosamples.agent.solr.stayalive=false --biosamples.agent.solr.queuetime=500
done

echo "Successfully completed"
