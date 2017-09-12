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
mvn -T 2C -P embl-ebi package

#cleanup any previous data
if [ $clean == 1 ]
then
	echo "Cleaning existing volumes"
	#remove any images, in case of out-of-date or corrupt images
	#docker-compose down --volumes --rmi --remove-orphans
	docker-compose down --volumes --remove-orphans
else
	docker-compose down --remove-orphans
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
