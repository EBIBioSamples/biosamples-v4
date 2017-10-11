#!/bin/bash
set -e

./docker-webapp.sh
set +e
rm docker-migration-test.log
set -e


#to do detailed comparison, use --comparison

time java -jar integration/target/integration-4.0.0-SNAPSHOT.jar --spring.profiles.active=migration \
--biosamples.migration.old="http://beans.ebi.ac.uk:9480/biosamples/xml/samples" \
--biosamples.migration.new="http://snowy.ebi.ac.uk:9083/biosamples/beta/xml/samples" \
--logging.file=docker-migration-test.log
