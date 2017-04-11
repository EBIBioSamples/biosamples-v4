# Docker

## Problems running neo4j container
Neo4j container wasn't able to start in my environment, and that was due to the 
environment variable `NEO4J_dbms_memory_heap_maxSize=2G` because my computer
was reserving just 2G of ram to docker, and so the container was crashing even before starting, 
without any meaningful log.

