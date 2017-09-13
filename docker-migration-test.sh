#!/bin/bash
set -e

./docker-webapp.sh

java -jar integration/target/integration-4.0.0.BSD-777-SNAPSHOT.jar --spring.profiles.active=migration --biosamples.client.uri=http://localhost:8081/biosamples/beta
