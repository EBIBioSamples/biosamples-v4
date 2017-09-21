#!/bin/bash
set -e

./docker-webapp.sh --clean

source docker-env.sh

#start up the agents
docker-compose up -d biosamples-agents-solr

#rsync -v -r --delete --include="*/" --include="sampletab.txt" --exclude="*" faulcon@beans.ebi.ac.uk:/ebi/microarray/home/biosamples/production/data/GSB /home/faulcon/Desktop/submissions
#rsync -v -r --delete --include="*/" --include="sampletab.txt" --exclude="*" faulcon@beans.ebi.ac.uk:/ebi/microarray/home/biosamples/production/data/ae /home/faulcon/Desktop/submissions
#rsync -v -r --delete --include="*/" --include="sampletab.txt" --exclude="*" faulcon@beans.ebi.ac.uk:/ebi/microarray/home/biosamples/production/data/sra /home/faulcon/Desktop/submissions
#rsync -v -r --delete --include="*/" --include="sampletab.txt" --exclude="*" faulcon@beans.ebi.ac.uk:/ebi/microarray/home/biosamples/production/data/GNC /home/faulcon/Desktop/submissions

java -jar pipelines/sampletab/target/pipelines-sampletab-4.0.0-SNAPSHOT.jar --biosamples.sampletab.uri="http://localhost:8082/biosamples/beta/sampletab/v4?setupdatedate" --biosamples.sampletab.path=/home/faulcon/Desktop/submissions/GSB
java -jar pipelines/sampletab/target/pipelines-sampletab-4.0.0-SNAPSHOT.jar --biosamples.sampletab.uri="http://localhost:8082/biosamples/beta/sampletab/v4?setupdatedate" --biosamples.sampletab.path=/home/faulcon/Desktop/submissions/ae
java -jar pipelines/sampletab/target/pipelines-sampletab-4.0.0-SNAPSHOT.jar --biosamples.sampletab.uri="http://localhost:8082/biosamples/beta/sampletab/v4?setupdatedate" --biosamples.sampletab.path=/home/faulcon/Desktop/submissions/sra
java -jar pipelines/sampletab/target/pipelines-sampletab-4.0.0-SNAPSHOT.jar --biosamples.sampletab.uri="http://localhost:8082/biosamples/beta/sampletab/v4?setupdatedate" --biosamples.sampletab.path=/home/faulcon/Desktop/submissions/GNC
