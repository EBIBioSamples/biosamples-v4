package uk.ac.ebi.biosamples.neo.repo;

import org.springframework.data.neo4j.repository.Neo4jRepository;

import uk.ac.ebi.biosamples.neo.model.NeoCurationApplication;

public interface NeoCurationApplicationRepository extends Neo4jRepository<NeoCurationApplication,Long> {

}
