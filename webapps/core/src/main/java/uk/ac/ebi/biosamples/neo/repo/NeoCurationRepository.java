package uk.ac.ebi.biosamples.neo.repo;

import org.springframework.data.neo4j.repository.Neo4jRepository;

import uk.ac.ebi.biosamples.neo.model.NeoCuration;

public interface NeoCurationRepository extends Neo4jRepository<NeoCuration,Long> {

}
