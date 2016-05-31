#!/bin/bash
set -e

PATH_MONGO=/home/faulcon/Desktop/mongodb/mongodb-linux-x86_64-ubuntu1404-3.2.6
PATH_MONGO_DATA=/home/faulcon/Desktop/mongodb/data
PATH_MONGO_LOG=/home/faulcon/Desktop/mongodb/mongo.log
PATH_RABBITMQ=/home/faulcon/Desktop/rabbitmq/rabbitmq_server-3.6.2
PATH_NEO4J=/home/faulcon/Desktop/neo4j/neo4j-community-3.0.1

PATH_HOME=/home/faulcon/work/prototype/bsd2017

#make sure everything is up to date and built
cd /home/faulcon/work/prototype/bsd2017
mvn clean package

#wipe any mongo data
rm -rf $PATH_MONGO_DATA/*

#Start mongoDB
$PATH_MONGO/bin/mongod --fork --dbpath $PATH_MONGO_DATA --logpath $PATH_MONGO_LOG

#Start rabbitMQ
$PATH_RABBITMQ/sbin/rabbitmq-server -detached

#wipe any neo4j data
rm -rf $PATH_NEO4J/data

#start neo4j
$PATH_NEO4J/bin/neo4j start

#wait for rabbitMQ to setup
echo "5s"; sleep 1
echo "4s"; sleep 1
echo "3s"; sleep 1
echo "2s"; sleep 1
echo "1s"; sleep 1

##wipe any rabbit data
$PATH_RABBITMQ/sbin/rabbitmqctl purge_queue biosamples.tobeloaded 
$PATH_RABBITMQ/sbin/rabbitmqctl purge_queue biosamples.tobeindexed.solr
$PATH_RABBITMQ/sbin/rabbitmqctl purge_queue biosamples.tobeindexed.neo4j

echo "5s"; sleep 1
echo "4s"; sleep 1
echo "3s"; sleep 1
echo "2s"; sleep 1
echo "1s"; sleep 1

echo Starting Submission WebApp...
nice java -jar $PATH_HOME/webapps/submission/target/webapps-submission-0.0.1-SNAPSHOT.war 2>&1 > $PATH_HOME/webapps/submission/webapps-submission-0.0.1-SNAPSHOT.log & PID_SUBS=$!

#wait for it all to settle down
echo "5s"; sleep 1
echo "4s"; sleep 1
echo "3s"; sleep 1
echo "2s"; sleep 1
echo "1s"; sleep 1


#POST to subs
curl -X POST -H "Content-Type: application/json" --data '@models/core/src/test/resources/TEST1.json' "http://localhost:8080/samples"
echo

#PUT to subs
curl -X PUT -H "Content-Type: application/json" --data '@models/core/src/test/resources/TEST1.json' "http://localhost:8080/samples"
echo

#POST to subs
curl -X POST -H "Content-Type: application/json" --data '@models/core/src/test/resources/TEST2.json' "http://localhost:8080/samples"
echo

#wait for it all to settle down
echo "5s"; sleep 1
echo "4s"; sleep 1
echo "3s"; sleep 1
echo "2s"; sleep 1
echo "1s"; sleep 1

echo Starting JPA agent...
nice java -jar $PATH_HOME/agents/jpa/target/agents-jpa-0.0.1-SNAPSHOT.jar 2>&1 > $PATH_HOME/agents/jpa/target/agents-jpa-0.0.1-SNAPSHOT.log & PID_LOADER=$!

echo "5s"; sleep 1
echo "4s"; sleep 1
echo "3s"; sleep 1
echo "2s"; sleep 1
echo "1s"; sleep 1

echo Starting Neo4J agent...
nice java -jar $PATH_HOME/agents/neo4j/target/agents-neo4j-0.0.1-SNAPSHOT.jar 2>&1 > $PATH_HOME/agents/neo4j/target/agents-neo4j-0.0.1-SNAPSHOT.log & PID_INDEXNEO=$!


#import NCBI pipeline
#echo Starting NCBI
#java -jar $PATH_HOME/pipelines/target/pipelines-0.0.1-SNAPSHOT.jar --ncbi

#while true; do THING; done;

#wait for user to interact with the system before carrying on
read -p "press return to continue"

#http://localhost:15672/#/queues/%2F/biosamples.tobeloaded
#http://localhost:28017

#Stop java procs
kill $PID_SUBS $PID_LOADER $PID_INDEXNEO

#Stop neo4j
$PATH_NEO4J/bin/neo4j stop

#Stop rabbitMQ
$PATH_RABBITMQ/sbin/rabbitmqctl stop

#Stop mongoDB
$PATH_MONGO/bin/mongod --shutdown --dbpath $PATH_MONGO_DATA