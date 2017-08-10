#!/bin/bash
set -e

clean=0
while [ "$1" != "" ]; do
    case $1 in
        -c | --clean )    		clean=1
                                ;;
    esac
    shift
done

#mvn -T 2C -Dmaven.test.skip=true clean package
mvn -T 2C -P embl-ebi clean package

set +e
docker-compose stop biosamples-pipelines-ena biosamples-pipelines-ncbi biosamples-pipelines-accession
docker-compose stop biosamples-agents-solr
docker-compose stop biosamples-webapps-core biosamples-webapps-sampletab biosamples-webapps-legacyxml 
docker-compose stop mongo solr rabbitmq

docker-compose rm -f -v biosamples-pipelines-ena biosamples-pipelines-ncbi biosamples-pipelines-accession
docker-compose rm -f -v biosamples-agents-solr
docker-compose rm -f -v biosamples-webapps-core biosamples-webapps-sampletab biosamples-webapps-legacyxml 
docker-compose rm -f -v mongo solr rabbitmq

#cleanup any previous data
echo "The clean value is $clean"
if [ $clean == 1 ]
then
	echo "Cleaning existing volumes"
	docker volume ls -q | grep mongo_data | xargs docker volume rm
	docker volume ls -q | grep solr_data | xargs docker volume rm
	docker volume ls -q | grep rabbitmq_data | xargs docker volume rm
	docker volume ls -q | grep logs | xargs docker volume rm

#remove any images, in case of out-of-date or corrupt images
#docker images -q | xargs -r docker rmi


fi
set -e

#make sure we have up-to-date jar files in the docker image
docker-compose build

#start up the webapps (and dependencies)
docker-compose up -d --remove-orphans solr rabbitmq mongo
echo "checking solr is up"
./http-status-check -u http://localhost:8983 -t 30
echo "checking rabbitmq is up"
./http-status-check -u http://localhost:15672 -t 30
echo "checking mongo is up"
./http-status-check -u http://localhost:27017 -t 30

docker-compose up -d biosamples-webapps-core biosamples-webapps-sampletab biosamples-webapps-legacyxml
echo "checking webapps-core is up"
./http-status-check -u http://localhost:8081/biosamples/beta/health -t 45
echo "checking webapps-sampletab is up"
./http-status-check -u http://localhost:8082/biosamples/beta/sampletab/health -t 30
echo "checking webapps-legacyxml is up"
./http-status-check -u http://localhost:8083/biosamples/beta/xml/health -t 30
