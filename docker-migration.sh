#!/bin/bash
set -e

./docker-webapp.sh --clean

source docker-env.sh

#start up the agents
docker-compose up -d biosamples-agents-solr

time docker-compose up biosamples-pipelines-ncbi > /dev/null
#pre-assign existing biosample accessions FIRST
time docker-compose up biosamples-pipelines-accession > /dev/null
time docker-compose up biosamples-pipelines-ena > /dev/null 
time docker-compose up biosamples-pipelines-sampletab > /dev/null
 
#time docker-compose up biosamples-pipelines-curation > /dev/null
