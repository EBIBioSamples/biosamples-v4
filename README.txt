Quickstart
==========

Install Maven and JDK 8
Install docker-compose https://docs.docker.com/compose/

`mvn package`

`docker-compose up`

public read API at http://localhost:8081/
public submission API at http://localhost:8083/
internal read/write API at http://localhost:8082/

internal RabbitMQ interface at http://localhost:15672/
internal Neo4J interface at http://localhost:7474/
internal Solr interface at http://localhost:8983/


Note: this will download around 1GB of docker containers


#POST to subs
curl -X POST -H "Content-Type: application/json" --data '@models/core/src/test/resources/TEST1.json' "http://localhost:8083/samples"

#POST to subs
curl -X POST -H "Content-Type: application/json" --data '@models/core/src/test/resources/TEST2.json' "http://localhost:8083/samples"

#echo \*\*\* Starting NCBI pipeline...
#nice java -jar $PATH_HOME/pipelines/target/pipelines-0.0.1-SNAPSHOT.jar --ncbi --biosamples.pipelines.ncbi.threadcount=8 2>&1 > $PATH_HOME/pipelines-ncbi.log & PID_NCBI=$! 

Getting started
===============

To checkout and compile the code, you will need Git, Maven, and a JDK 8. On ubuntu-based Linux distributions (16.04 or higher) you can do this with:

`sudo apt-get install maven git default-jdk`

Then you can check out and compile the code with:

`git clone https://github.com/EBIBioSamples/biosamples-v4 biosamples`
`cd biosamples`
`mvn package`

Note: This will require a large download of Spring dependencies.

At that point, you will have a local compiled version of all the biosamples tools.

To start a copy running on the local machine (e.g. to test any changes you have made) you can use Docker and Docker-compose. https://docs.docker.com/compose/

You can use `docker-compose up` to start all the services, or you can bring them up and down at will indivdually. 
See docker-compose.yml file for more information on service names and dependencies.



Developing
==========

You will need:
 - Java 8 JDK (preferably Oracle)
 - Maven 3+
 - Git
 - Docker and docker-compose 
 
