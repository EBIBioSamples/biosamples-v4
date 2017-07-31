#!/bin/bash
set -e

./docker-webapp.sh --clean

source docker-env.sh

#start up the agents
docker-compose up -d biosamples-agents-solr biosamples-agents-curation

#import from NCBI
echo "Importing from NCBI"
date
ARGS=
ARGS="$ARGS --biosamples.client.uri=http://localhost:8081/biosamples/beta" 
ARGS="$ARGS --biosamples.ncbi.file=/home/faulcon/Desktop/biosample_set.xml.gz"
ARGS="$ARGS --logging.file=./docker/logs/pipelines-ncbi.log"
ARGS="$ARGS --biosamples.threadcount.max=16"
#wget -O /home/faulcon/Desktop/biosample_set.xml.gz http://ftp.ncbi.nih.gov/biosample/biosample_set.xml.gz 
time java -jar pipelines/ncbi/target/pipelines-ncbi-4.0.0-SNAPSHOT.jar $ARGS > /dev/null
echo "Imported from NCBI"
date

#pre-assign existing biosample accessions FIRST
echo "Importing accessions"
date
ARGS=
ARGS="$ARGS --biosamples.client.uri=http://localhost:8081/biosamples/beta" 
ARGS="$ARGS --logging.file=./docker/logs/pipelines-accession.log"
ARGS="$ARGS --biosamples.threadcount.max=16"
time java -jar pipelines/accession/target/pipelines-accession-4.0.0-SNAPSHOT.jar $ARGS > /dev/null
echo "Imported accessions"
date

echo "Importing from ENA"
date
ARGS=
ARGS="$ARGS --biosamples.client.uri=http://localhost:8081/biosamples/beta" 
ARGS="$ARGS --logging.file=./docker/logs/pipelines-ena.log"
ARGS="$ARGS --biosamples.threadcount.max=16"
time java -jar pipelines/ena/target/pipelines-ena-4.0.0-SNAPSHOT.jar $ARGS > /dev/null
echo "Imported from ENA"
date

echo "Importing SampleTab submissions"
date
export SUBS_HOME=/home/faulcon/Desktop/submissions/GSB
#rsync -zarv --delete --prune-empty-dirs --include="*/" --include="sampletab.toload.txt" --exclude="*" ebi-cli.ebi.ac.uk:/ebi/microarray/home/biosamples/production/data/GSB/ $SUBS_HOME
ARGS=
ARGS="$ARGS --biosamples.sampletab.uri=http://localhost:8082/biosamples/beta/sampletab/v4" 
ARGS="$ARGS --biosamples.sampletab.path=/home/faulcon/Desktop/submissions/GSB" 
ARGS="$ARGS --logging.file=./docker/logs/pipelines-sampletab.gsb.log"
ARGS="$ARGS --biosamples.threadcount.max=16"
time java -jar pipelines/sampletab/target/pipelines-sampletab-4.0.0-SNAPSHOT.jar $ARGS > /dev/null
echo "Imported SampleTab submissions"
date

echo "Importing ArrayExpress submissions"
date
export SUBS_HOME=/home/faulcon/Desktop/submissions/GAE
#rsync -zarv --delete --prune-empty-dirs --include="*/" --include="sampletab.toload.txt" --exclude="*" ebi-cli.ebi.ac.uk:/ebi/microarray/home/biosamples/production/data/ae/ $SUBS_HOME
ARGS=
ARGS="$ARGS --biosamples.sampletab.uri=http://localhost:8082/biosamples/beta/sampletab/v4" 
ARGS="$ARGS --biosamples.sampletab.path=/home/faulcon/Desktop/submissions/GAE" 
ARGS="$ARGS --logging.file=./docker/logs/pipelines-sampletab.gae.log"
ARGS="$ARGS --biosamples.threadcount.max=16"
time java -jar pipelines/sampletab/target/pipelines-sampletab-4.0.0-SNAPSHOT.jar $ARGS > /dev/null
echo "Imported ArrayExpress submissions"
date


echo "Applying curation"
date
ARGS=
ARGS="$ARGS --biosamples.client.uri=http://localhost:8081/biosamples/beta" 
ARGS="$ARGS --logging.file=./docker/logs/pipelines-curation.log"
ARGS="$ARGS --biosamples.threadcount.max=16"
time java -jar pipelines/curation/target/pipelines-curation-4.0.0-SNAPSHOT.jar $ARGS > /dev/null
echo "Applied curation"
date
