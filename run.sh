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

#shutdown mongo if running
echo \*\*\* Shutting down MongoDB
set +e
$PATH_MONGO/bin/mongod --shutdown --dbpath $PATH_MONGO_DATA
set -e
#wipe any mongo data
echo \*\*\* Wiping MongoDB
rm -rf $PATH_MONGO_DATA/*
#startup mongo
echo \*\*\* Starting MongoDB
nice $PATH_MONGO/bin/mongod --fork --dbpath $PATH_MONGO_DATA --logpath $PATH_MONGO_LOG


#shutdown rabbitMQ if running
echo \*\*\* Shutting down RabbitMQ
$PATH_RABBITMQ/sbin/rabbitmqctl stop
sleep 1
#startup rabbitMQ
echo \*\*\* Starting RabbitMQ
set +e
nice $PATH_RABBITMQ/sbin/rabbitmq-server -detached
set -e

#shutdown neo4j if running
echo \*\*\* Shutting down Neo4J
$PATH_NEO4J/bin/neo4j stop
#wipe any neo4j data
echo \*\*\* Wiping Neo4J
rm -rf $PATH_NEO4J/data/databases/graph.db
#start neo4j
echo \*\*\* Starting Neo4J
$PATH_NEO4J/bin/neo4j start

#wait for everything to setup
echo "5s"; sleep 1
echo "4s"; sleep 1
echo "3s"; sleep 1
echo "2s"; sleep 1
echo "1s"; sleep 1

##wipe any rabbit data
echo \*\*\* Purging RabbitMQ queues
$PATH_RABBITMQ/sbin/rabbitmqctl purge_queue biosamples.tobeloaded 
$PATH_RABBITMQ/sbin/rabbitmqctl purge_queue biosamples.tobeindexed.solr
$PATH_RABBITMQ/sbin/rabbitmqctl purge_queue biosamples.tobeindexed.neo4j

echo \*\*\* Starting Submission WebApp...
nice java -jar $PATH_HOME/webapps/submission/target/webapps-submission-0.0.1-SNAPSHOT.war 2>&1 > $PATH_HOME/submission.log & PID_SUBS=$!


#check if its running
echo "15s"; sleep 5
echo "10s"; sleep 2
echo "8s"; sleep 2
echo "6s"; sleep 1
echo "5s"; sleep 1
echo "4s"; sleep 1
echo "3s"; sleep 1
echo "2s"; sleep 1
echo "1s"; sleep 1
curl localhost:8080/repository


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

echo \*\*\* Starting NCBI pipeline...
nice java -jar $PATH_HOME/pipelines/target/pipelines-0.0.1-SNAPSHOT.jar --ncbi --biosamples.pipelines.ncbi.threadcount=8 2>&1 > $PATH_HOME/pipelines-ncbi.log & PID_NCBI=$! 


echo \*\*\* Starting JPA agent...
nice java -jar $PATH_HOME/agents/jpa/target/agents-jpa-0.0.1-SNAPSHOT.jar --spring.rabbitmq.listener.max-concurrency=8  2>&1 > $PATH_HOME/agents-jpa.log & PID_LOADER=$!

echo \*\*\* Starting Neo4J agent...
nice java -jar $PATH_HOME/agents/neo4j/target/agents-neo4j-0.0.1-SNAPSHOT.jar --spring.rabbitmq.listener.max-concurrency=8  2>&1 > $PATH_HOME/agents-neo4j.log & PID_INDEXNEO=$!

#while true; do THING; done;

#wait for user to interact with the system before carrying on
read -p "press return to continue"

#http://localhost:15672/#/queues/%2F/biosamples.tobeloaded
#http://localhost:28017

set +e
echo \*\*\* Stopping Submission WebApp...
kill $PID_SUBS

echo \*\*\* Stopping NCBI pipeline...
kill $PID_NCBI 

echo \*\*\* Stopping JPA agent...
kill $PID_LOADER 

echo \*\*\* Stopping Neo4J agent...
kill $PID_INDEXNEO 

ps aux | grep bsd2017