#!/bin/bash
set -e

docker-compose down -v --remove-orphans &

mvn -T 2C clean package

wait

#cleanup any previous data
docker volume ls -q | xargs -r docker volume rm
#remove any images, in case of out-of-date or corrupt images
#docker images -q | xargs -r docker rmi

#make sure we have up-to-date jar files in the docker image
docker-compose build

#start up logging infrastructure
docker-compose up -d elasticsearch logstash kibana
echo "checking elasticsearch is up" 
./http-status-check -u http://localhost:9200 -t 30
echo "checking logstash is up" 
./http-status-check -u http://localhost:9600 -t 30
echo "checking kibana is up"
#takes a bit longer, so sleep first
sleep 60
./http-status-check -u http://localhost:5601 -t 30

#start up the webapps (and dependencies)
docker-compose up -d biosamples-webapps-core biosamples-webapps-sampletab

echo "checking solr is up"
./http-status-check -u http://localhost:8983 -t 30
echo "checking neo4j is up"
./http-status-check -u http://localhost:7474 -t 30

echo "checking webapps-core is up"
#would like to check on /health but currently it is bugged so solr is always down
./http-status-check -u http://localhost:8081/biosamples/beta/actuator -t 30
./http-status-check -u http://localhost:8081/biosamples/beta/samples -t 30
echo "checking webapps-sampletab is up"
./http-status-check -u http://localhost:8082/biosamples/beta/health -t 30

#run phase 1 (pre-agent) testing
java -jar integration/target/integration-4.0.0-SNAPSHOT.jar --phase=1 --biosamples.submissionuri=http://localhost:8081/biosamples/beta --biosamples.submissionuri.sampletab=http://localhost:8082/biosamples/beta

#docker-compose run --service-ports biosamples-agents-neo4j java -jar agents-neo4j-4.0.0-SNAPSHOT.jar --biosamples.agent.neo4j.stayalive=false &
#docker-compose run --service-ports biosamples-agents-solr java -jar agents-solr-4.0.0-SNAPSHOT.jar --biosamples.agent.solr.stayalive=false &
#docker-compose run --service-ports biosamples-agents-curation java -jar agents-curation-4.0.0-SNAPSHOT.jar --biosamples.agent.curation.stayalive=false &
#wait

docker-compose run --service-ports biosamples-agents-neo4j java -jar agents-neo4j-4.0.0-SNAPSHOT.jar --biosamples.agent.neo4j.stayalive=false 
docker-compose run --service-ports biosamples-agents-solr java -jar agents-solr-4.0.0-SNAPSHOT.jar --biosamples.agent.solr.stayalive=false 
docker-compose run --service-ports biosamples-agents-curation java -jar agents-curation-4.0.0-SNAPSHOT.jar --biosamples.agent.curation.stayalive=false

echo "sleeping for 10 seconds..."
sleep 10

#run phase 2 (post-agent) testing
java -jar integration/target/integration-4.0.0-SNAPSHOT.jar --phase=2 --biosamples.submissionuri=http://localhost:8081/biosamples/beta --biosamples.submissionuri.sampletab=http://localhost:8082/biosamples/beta

echo "Sucessfullly completed"