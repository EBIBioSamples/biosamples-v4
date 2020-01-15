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

#cleanup any previous data
if [ $clean == 1 ]
then
	echo "Cleaning existing volumes"
	#remove any images, in case of out-of-date or corrupt images
	#docker-compose down --volumes --remove-orphans
	docker-compose down --volumes --rmi local --remove-orphans
	mvn -T 2C -P embl-ebi clean package -Dembedmongo.wait -Dtest=!uk.ac.ebi.biosamples.ols -DfailIfNoTests=false
else
	docker-compose down --rmi local --remove-orphans
	mvn -T 2C -P embl-ebi clean package -Dembedmongo.wait -Dtest=!uk.ac.ebi.biosamples.ols -DfailIfNoTests=false
fi
set -e

#make sure we have up-to-date jar files in the docker image
docker-compose build

#start up the webapps (and dependencies)
docker-compose up -d --remove-orphans solr rabbitmq mongo json-schema-validator
echo "checking solr is up"
./http-status-check -u http://localhost:8983 -t 30
echo "checking rabbitmq is up"
./http-status-check -u http://localhost:15672 -t 30
echo "checking mongo is up"
./http-status-check -u http://localhost:27017 -t 30
echo "checking json-schema-validator is up"
./http-status-check -u http://localhost:8085/validate -t 30


#configure solr
curl http://localhost:8983/solr/samples/config -H 'Content-type:application/json' -d'{"set-property" : {"updateHandler.autoCommit.maxTime":1000, "updateHandler.autoCommit.openSearcher":"true", "updateHandler.autoSoftCommit.maxDocs":1, "query.documentCache.size":1024, "query.filterCache.size":1024, "query.filterCache.autowarmCount":128, "query.queryResultCache.size":1024, "query.queryResultCache.autowarmCount":128}}'

#create an api key for submitting test sampletab documents
docker-compose run --rm mongo mongo --eval 'db.mongoSampleTabApiKey.insert({"_id" : "fooqwerty", "_class" : "uk.ac.ebi.biosamples.mongo.model.MongoSampleTabApiKey", "userName" : "BioSamples", "publicEmail" : "", "publicUrl" : "", "contactName" : "", "contactEmail" : "", "aapDomain" : "123456789abcdef" });' mongo:27017/biosamples
#profile any queries that take longer than 100 ms
docker-compose run --rm mongo mongo --eval 'db.setProfilingLevel(1)' mongo:27017/biosamples


docker-compose up -d biosamples-webapps-core biosamples-webapps-sampletab biosamples-webapps-legacyxml biosamples-webapps-legacyjson
sleep 40
echo "checking webapps-core is up"
./http-status-check -u http://localhost:8081/biosamples/health -t 600
echo "checking webapps-sampletab is up"
./http-status-check -u http://localhost:8082/biosamples/sampletab/health -t 60
echo "checking webapps-legacyxml is up"
./http-status-check -u http://localhost:8083/biosamples/xml/health -t 60
echo "checking webapps-legacyjson is up"
./http-status-check -u http://localhost:8084/biosamples/api/health -t 60
