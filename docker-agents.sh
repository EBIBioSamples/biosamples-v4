#!/bin/bash
set -e

docker-compose run --rm --service-ports biosamples-agents-solr java -jar agents-solr-4.2.5-RC2.jar
echo "Successfully runned agents"
