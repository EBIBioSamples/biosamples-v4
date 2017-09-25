#!/bin/bash
set -e

./docker-webapp.sh --clean

source docker-env.sh

#start up the agents
docker-compose up -d biosamples-agents-solr

time docker-compose up biosamples-pipelines-ncbi

#pre-assign existing biosample accessions FIRST
time docker-compose up biosamples-pipelines-accession
time docker-compose up biosamples-pipelines-ena 
time docker-compose up biosamples-pipelines-sampletab
 
#time docker-compose up biosamples-pipelines-curation > /dev/null
