#!/bin/bash
set -e

docker-compose run --rm mongo mongo --eval 'db.mongoSampleTabApiKey.insert({"_id" : "fooqwerty", "_class" : "uk.ac.ebi.biosamples.mongo.model.MongoSampleTabApiKey", "userName" : "BioSamples", "publicEmail" : "", "publicUrl" : "", "contactName" : "", "contactEmail" : "", "aapDomain" : "123456789abcdef" });' mongo:27017

docker-compose up -d biosamples-agents-solr

for X in "$@"
do
  docker-compose run --rm --service-ports biosamples-integration java -jar integration-4.0.8-SNAPSHOT.jar --phase=$X $ARGS $@
  sleep 30 #solr is configured to commit every 5 seconds

done

docker-compose up -d biosamples-agents-solr

echo "Successfully completed"

