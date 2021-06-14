#!/bin/bash
set -e

docker-compose run --rm --service-ports biosamples-agents-solr java -jar agents-solr-5.1.4-RC4.jar
echo "Successfully runned agents"
