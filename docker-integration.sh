#!/bin/bash
set -e

./docker-webapp.sh --clean

#create an api key for submitting test sampletab documents
docker-compose run --rm mongo mongo --eval 'db.mongoSampleTabApiKey.insert({"_id" : "fooqwerty", "_class" : "uk.ac.ebi.biosamples.mongo.model.MongoSampleTabApiKey", "userName" : "BioSamples", "publicEmail" : "", "publicUrl" : "", "contactName" : "", "contactEmail" : "", "aapDomain" : "123456789abcdef" });' mongo:27017/biosamples

#create an API key like:
#"".join([random.choice("ABCDEFGHKLMNPRTUWXY0123456789") for x in xrange(16)])
#


docker-compose up -d biosamples-agents-solr


#--spring.profiles.active=default,big

for X in 1 2 3 4 5
do
  #java -jar integration/target/integration-4.0.0-SNAPSHOT.jar --phase=$X $ARGS $@
  docker-compose run --rm --service-ports biosamples-integration java -jar integration-4.0.8.jar --phase=$X $ARGS $@
  sleep 30 #solr is configured to commit every 5 seconds

done

#leave the agent up at the end
docker-compose up -d biosamples-agents-solr

echo "Successfully completed"
