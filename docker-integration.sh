#!/bin/bash
set -e

docker-compose down -v --remove-orphans &

mvn -T 2C -U clean package

wait

#docker volume ls -q | xargs -r docker volume rm
#docker images -q | xargs -r docker rmi

docker-compose build
docker-compose up -d biosamples-webapps-core biosamples-webapps-sampletab

#would like to check on /health but currently it is bugged so solr is always down
./http-status-check -u http://localhost:8081/biosamples/beta/actuator -t 30
./http-status-check -u http://localhost:8081/biosamples/beta/samples -t 30
./http-status-check -u http://localhost:8082/biosamples/beta/health -t 30

java -jar integration/target/integration-4.0.0-SNAPSHOT.jar --phase=1 --biosamples.submissionuri=http://localhost:8081/biosamples/beta --biosamples.submissionuri.sampletab=http://localhost:8082/biosamples/beta

docker-compose run --service-ports biosamples-agents-neo4j java -jar agents-neo4j-4.0.0-SNAPSHOT.jar --biosamples.agent.neo4j.stayalive=false
docker-compose run --service-ports biosamples-agents-solr java -jar agents-solr-4.0.0-SNAPSHOT.jar --biosamples.agent.solr.stayalive=false
docker-compose run --service-ports biosamples-agents-curation java -jar agents-curation-4.0.0-SNAPSHOT.jar --biosamples.agent.curation.stayalive=false
wait

echo "sleeping for 10 seconds..."
sleep 10

java -jar integration/target/integration-4.0.0-SNAPSHOT.jar --phase=2 --biosamples.submissionuri=http://localhost:8081/biosamples/beta --biosamples.submissionuri.sampletab=http://localhost:8082/biosamples/beta

