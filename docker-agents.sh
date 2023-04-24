#!/bin/bash
set -e

docker-compose run --rm --service-ports biosamples-agents-solr java -jar agents-solr-5.2.17-SNAPSHOT.jar
echo "Successfully runned agents"
