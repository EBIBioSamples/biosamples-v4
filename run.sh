#!/bin/bash
set -e

PATH_MONGO=/home/faulcon/Desktop/mongodb/mongodb-linux-x86_64-ubuntu1404-3.2.6
PATH_MONGO_DATA=/home/faulcon/Desktop/mongodb/data
PATH_MONGO_LOG=/home/faulcon/Desktop/mongodb/mongo.log
PATH_RABBITMQ=/home/faulcon/Desktop/rabbitmq/rabbitmq_server-3.6.2
PATH_HOME=/home/faulcon/work/prototype/bsd2017

#make sure everything is up to date and built
cd /home/faulcon/work/prototype/bsd2017
mvn clean package

#wipe any mongo data
rm -rf $PATH_MONGO_DATA/*

##wipe any rabbit data

#Start mongoDB
$PATH_MONGO/bin/mongod --fork --dbpath $PATH_MONGO_DATA --logpath $PATH_MONGO_LOG

#Start rabbitMQ
$PATH_RABBITMQ/sbin/rabbitmq-server -detached

#wait for rabbitMQ to setup
echo "10s"; sleep 5
echo "5s"; sleep 1
echo "4s"; sleep 1
echo "3s"; sleep 1
echo "2s"; sleep 1
echo "1s"; sleep 1

#Start subs
echo Starting submission API...
java -jar $PATH_HOME/subs/target/subs-0.0.1-SNAPSHOT.war 2>&1 > $PATH_HOME/subs/target/subs-0.0.1-SNAPSHOT.log & PID_SUBS=$!

echo "5s"; sleep 1
echo "4s"; sleep 1
echo "3s"; sleep 1
echo "2s"; sleep 1
echo "1s"; sleep 1

#Start loader
echo Starting loader...
java -jar $PATH_HOME/loader/target/loader-0.0.1-SNAPSHOT.jar --always 2>&1 > $PATH_HOME/loader/target/loader-0.0.1-SNAPSHOT.log & PID_LOADER=$!

#wait for it all to settle down
echo "5s"; sleep 1
echo "4s"; sleep 1
echo "3s"; sleep 1
echo "2s"; sleep 1
echo "1s"; sleep 1


#POST to subs
curl -X POST -H "Content-Type: application/json" --data '@models/src/test/resources/TEST1.json' "http://localhost:8080/samples"
echo

#PUT to subs
curl -X PUT -H "Content-Type: application/json" --data '@models/src/test/resources/TEST1.json' "http://localhost:8080/samples"
echo

#while true; do THING; done;

#wait for user to interact with the system before carrying on
read -p "press return to continue"

#Stop java procs
kill $PID_SUBS $PID_LOADER

#Stop rabbitMQ
$PATH_RABBITMQ/sbin/rabbitmqctl stop

#Stop mongoDB
$PATH_MONGO/bin/mongod --shutdown --dbpath $PATH_MONGO_DATA