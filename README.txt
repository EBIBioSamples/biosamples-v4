Quickstart
==========

Install Maven and JDK 8
Install docker-compose https://docs.docker.com/compose/

`mvn package`

`docker-compose up -d`

public read API at http://localhost:8081/
public submission API at http://localhost:8083/
internal read/write API at http://localhost:8082/

internal RabbitMQ interface at http://localhost:15672/
internal Neo4J interface at http://localhost:7474/
internal Solr interface at http://localhost:8983/


Note: this will download around 1GB of docker containers


curl -X PUT -H "Content-Type: application/json" --data @models/core/src/test/resources/TEST1.json "http://localhost:8081/samples/TEST1"
curl -X GET -H "Content-Type: application/json" "http://localhost:8081/samples/TEST1"

Getting started
===============

To checkout and compile the code, you will need Git, Maven, and a JDK 8. On ubuntu-based Linux distributions (16.04 or higher) you can do this with:

`sudo apt-get install maven git default-jdk`

Then you can check out and compile the code with:

`git clone https://github.com/EBIBioSamples/biosamples-v4 biosamples`
`cd biosamples`
`mvn -T 2C package`

Note: This will require a large download of Spring dependencies.
Note: This uses up to two threads per core of your machine.

At that point, you will have a local compiled version of all the biosamples tools.

To start a copy running on the local machine (e.g. to test any changes you have made) you can 
use Docker and Docker-compose. https://docs.docker.com/compose/

You can use `docker-compose up` to start all the services, or you can bring them up and down at 
will individually. See docker-compose.yml file for more information on service names and dependencies.


By default, the pipelines will not be run. They can be manually triggered as follows:

NCBI
----

Download the XML dump (~400Mb) to the current directory:

`wget http://ftp.ncbi.nih.gov/biosample/biosample_set.xml.gz`

Run the pipeline to send the data to the submission API via REST

`java -jar pipelines/target/pipelines-4.0.0-SNAPSHOT.jar --ncbi`


Developing
==========

Docker can be run from within a virtual machine e.g VirtualBox. This is useful if it causes any 
problems for your machine or if you have an OS that is not supported.

You might want to mount the virtual machines directory with the host, so you can work in a standard 
IDE outside of the VM. VirtualBox supports this.

If you ware using a virtual machine, you might also want to configure docker-compose to start by 
default. 

As you make changes to the code, you can recompile it via Maven with:

`mvn -T 2C package`

And to get the new packages into the docker containers you will need to rebuild containers with:

`docker-compose build`

If needed, you can rebuild just a single container by specifying its name e.g.

`docker-compose build biosamples-pipelines`

To start a service, using docker compose will also start and dependent services it requires e.g.

`docker-compose up biosamples-webapp-api`

will also start solr, neo4j, mongo, and rabbitmq

If you want to connect debugging tools to the java applications running inside docker containers, 
see instructions at http://www.jamasoftware.com/blog/monitoring-java-applications/

Beware, Docker tar's and copies all the files on the filesystem from the location of docker-compose 
down. If you have data files there (e.g. downloads from ncbi, docker volumes) then that process can
take so long as to make using Docker impractical.
 
 
Known problems
==============

This relies on spring-data-rest for exposing the repositories as REST endpoints. However, that
can only serve JSON and cannot serve XML. Currently, some type negotiation backend is implemented
but this is not exposed in anyway.

When repeatedly sending JSON because it is a list of things with optional components, the optional 
parts can become mixed if the list ordering changes. Maybe this can be remedied by using map of 
attribute types instead?