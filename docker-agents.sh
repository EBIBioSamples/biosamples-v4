#!/bin/bash
set -e

#docker-compose run --rm --service-ports biosamples-agents-neo4j java -jar agents-neo4j-4.0.0-SNAPSHOT.jar --biosamples.agent.neo4j.stayalive=false --biosamples.agent.solr.queuetime=500  
#docker-compose run --rm --service-ports biosamples-agents-curation java -jar agents-curation-4.0.0-SNAPSHOT.jar --biosamples.agent.curation.stayalive=false
docker-compose run --rm --service-ports biosamples-agents-solr java -jar agents-solr-4.0.5-SNAPSHOT.jar --biosamples.agent.solr.stayalive=false --biosamples.agent.solr.queuetime=500
echo "Successfully runned agents"
