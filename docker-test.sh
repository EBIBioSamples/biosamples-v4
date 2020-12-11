#!/bin/bash
set -e

docker-compose up -d biosamples-agents-solr

for X in "$@"
do
<<<<<<< HEAD
  docker-compose run --rm --service-ports biosamples-integration java -jar integration-5.0.3-SNAPSHOT.jar --phase=$X $ARGS $@
=======
  docker-compose run --rm --service-ports biosamples-integration java -jar integration-5.0.2-RC1.jar --phase=$X $ARGS $@
>>>>>>> remotes/origin/master
  sleep 30 #solr is configured to commit every 5 seconds

done

docker-compose up -d biosamples-agents-solr

echo "Successfully completed"

