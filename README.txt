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



Developing
==========

You will need:
 - Java 8 JDK (preferably Oracle)
 - Maven 3+
 - Git
 - Docker and docker-compose 
 
