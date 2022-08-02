#!/bin/bash
set -e

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
