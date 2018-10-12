#!/bin/bash
set -e
  
docker-compose run --rm --service-ports biosamples-agents-solr java -jar agents-solr-4.1.6-rc2-SNAPSHOT.jar
echo "Successfully runned agents"
