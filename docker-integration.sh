#!/bin/bash
set -e

./docker-webapp.sh

#run phase 1 (pre-agent) testing
java -jar integration/target/integration-4.0.0-SNAPSHOT.jar --phase=1 --biosamples.submissionuri=http://localhost:8081/biosamples/beta --biosamples.submissionuri.sampletab=http://localhost:8082/biosamples/beta

<<<<<<< HEAD
#docker-compose run --service-ports biosamples-agents-neo4j java -jar agents-neo4j-4.0.0-SNAPSHOT.jar --biosamples.agent.neo4j.stayalive=false &
#docker-compose run --service-ports biosamples-agents-solr java -jar agents-solr-4.0.0-SNAPSHOT.jar --biosamples.agent.solr.stayalive=false &
#docker-compose run --service-ports biosamples-agents-curation java -jar agents-curation-4.0.0-SNAPSHOT.jar --biosamples.agent.curation.stayalive=false &
#wait

docker-compose run --rm --service-ports biosamples-agents-neo4j java -jar agents-neo4j-4.0.0-SNAPSHOT.jar --biosamples.agent.neo4j.stayalive=false 
docker-compose run --rm --service-ports biosamples-agents-solr java -jar agents-solr-4.0.0-SNAPSHOT.jar --biosamples.agent.solr.stayalive=false 
docker-compose run --rm --service-ports biosamples-agents-curation java -jar agents-curation-4.0.0-SNAPSHOT.jar --biosamples.agent.curation.stayalive=false

echo "sleeping for 10 seconds..."
sleep 10

#run phase 2 (post-agent) testing
java -jar integration/target/integration-4.0.0-SNAPSHOT.jar --phase=2 --biosamples.submissionuri=http://localhost:8081/biosamples/beta --biosamples.submissionuri.sampletab=http://localhost:8082/biosamples/beta

echo "Successfullly completed"
