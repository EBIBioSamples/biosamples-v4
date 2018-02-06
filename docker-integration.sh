#!/bin/bash
set -e

./docker-webapp.sh --clean

docker-compose run --rm mongo mongo --eval 'db.mongoSampleTabApiKey.insert({"_id" : "fooqwerty", "_class" : "uk.ac.ebi.biosamples.mongo.model.MongoSampleTabApiKey", "userName" : "BioSamples", "publicEmail" : "", "publicUrl" : "", "contactName" : "", "contactEmail" : "", "aapDomain" : "123456789abcdef" });' mongo:27017

for X in 1 2 3 4 5
do
  #java -jar integration/target/integration-4.0.0-SNAPSHOT.jar --phase=$X $ARGS $@
  docker-compose run --rm --service-ports biosamples-integration java -jar integration-4.0.4-SNAPSHOT.jar --phase=$X $ARGS $@
  docker-compose run --rm --service-ports biosamples-agents-solr java -jar agents-solr-4.0.4-SNAPSHOT.jar --biosamples.agent.solr.stayalive=false --biosamples.agent.solr.queuetime=500
done

#leave the agent up at the end
docker-compose up -d biosamples-agents-solr

echo "Successfully completed"
