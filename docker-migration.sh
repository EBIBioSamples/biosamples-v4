#!/bin/bash
set -e

./docker-webapp.sh

source docker-env.sh

#start up the agents
docker-compose up -d biosamples-agents-solr biosamples-agents-curation
docker-compose scale biosamples-agents-solr=5

#pre-assign existing biosample accessions FIRST
ARGS=
ARGS="$ARGS --biosamples.client.uri=http://localhost:8081/biosamples/beta" 
ARGS="$ARGS --logging.file=/logs/pipelines-accession.log"
time java -jar pipelines/accession/target/pipelines-accession-4.0.0-SNAPSHOT.jar $ARGS

#import sampletab submissions
echo "Importing SampleTab submissions"
export SUBS_HOME=/home/faulcon/Desktop/submissions
#rsync -zarv --prune-empty-dirs --include="*/" --include="sampletab.pre.txt" --exclude="*" ebi-cli.ebi.ac.uk:/ebi/microarray/home/biosamples/production/data/GSB/ $SUBS_HOME
ls $SUBS_HOME/*/sampletab.pre.txt | xargs -n 1 -P 8 -I {} curl -X POST -H "Content-Type: application/text" --data-binary @{} http://localhost:8082/biosamples/beta/sampletab/v4
echo "Imported SampleTab submissions"

#import from ena
echo "Importing from ENA"
ARGS=
ARGS="$ARGS --biosamples.client.uri=http://localhost:8081/biosamples/beta" 
ARGS="$ARGS --logging.file=./docker/logs/pipelines-ena.log"
time java -jar pipelines/ena/target/pipelines-ena-4.0.0-SNAPSHOT.jar $ARGS
echo "Imported from ENA"

#import from NCBI
echo "Importing from NCBI"
ARGS=
ARGS="$ARGS --biosamples.client.uri=http://localhost:8081/biosamples/beta" 
ARGS="$ARGS --biosamples.ncbi.file=/home/faulcon/Desktop/biosample_set.xml.gz"
ARGS="$ARGS --logging.file=./docker/logs/pipelines-ncbi.log"
#wget -O /home/faulcon/Desktop/biosample_set.xml.gz http://ftp.ncbi.nih.gov/biosample/biosample_set.xml.gz 
time java -jar pipelines/ncbi/target/pipelines-ncbi-4.0.0-SNAPSHOT.jar $ARGS
echo "Imported from NCBI"

