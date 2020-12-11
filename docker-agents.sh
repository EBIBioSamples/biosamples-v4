#!/bin/bash
set -e

<<<<<<< HEAD
docker-compose run --rm --service-ports biosamples-agents-solr java -jar agents-solr-5.0.3-SNAPSHOT.jar
=======
docker-compose run --rm --service-ports biosamples-agents-solr java -jar agents-solr-5.0.2-RC1.jar
>>>>>>> remotes/origin/master
echo "Successfully runned agents"
