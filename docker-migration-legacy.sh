#!/bin/bash
set -e

./docker-webapp.sh --clean

source docker-env.sh

#start up the agents
docker-compose up -d biosamples-agents-solr

export BIOSAMPLES_CLIENT_URI="http://biosamples-webapps-core:8081/biosamples/beta"

java -jar pipelines/pipelines-legacyxml/target/pipelines-legacyxml-4.0.0-SNAPSHOT.jar import ./biosamples.beans.xml.gz ./ 
