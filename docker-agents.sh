#!/bin/bash
set -e

docker-compose run --rm --service-ports biosamples-agents-solr java -jar agents-solr-v4.2.1.jar
echo "Successfully runned agents"
