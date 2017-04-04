#!/bin/bash
set -e

java -jar integration/target/integration-4.0.0-SNAPSHOT.jar --migration --biosamples.submissionuri=http://localhost:8081/biosamples/beta
