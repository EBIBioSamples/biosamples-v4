#!/bin/bash
set -e

./docker-webapp.sh

java -jar integration/target/integration-4.0.0-SNAPSHOT.jar --spring.profiles.active=migration
