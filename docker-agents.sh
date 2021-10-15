#!/bin/bash
set -e

docker-compose run --rm --service-ports biosamples-agents-solr java -jar agents-solr-5.1.11-RC1.jar
echo "Successfully runned agents"
