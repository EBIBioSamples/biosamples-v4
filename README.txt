Quickstart
==========

Install docker-compose https://docs.docker.com/compose/

`docker-compose up`

(Note: Currently only the services and agents are configured. APIs will not be started.)

RabbitMQ http://localhost:15672/
Neo4J http://localhost:7474/
Solr http://localhost:8983/



#POST to subs
curl -X POST -H "Content-Type: application/json" --data '@models/core/src/test/resources/TEST1.json' "http://localhost:8081/samples"

#POST to subs
curl -X POST -H "Content-Type: application/json" --data '@models/core/src/test/resources/TEST2.json' "http://localhost:8081/samples"

#echo \*\*\* Starting NCBI pipeline...
#nice java -jar $PATH_HOME/pipelines/target/pipelines-0.0.1-SNAPSHOT.jar --ncbi --biosamples.pipelines.ncbi.threadcount=8 2>&1 > $PATH_HOME/pipelines-ncbi.log & PID_NCBI=$! 
