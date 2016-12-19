package uk.ac.ebi.biosamples.neo.repo;

import org.springframework.data.neo4j.repository.GraphRepository;

import uk.ac.ebi.biosamples.neo.model.NeoRelationship;

public interface NeoRelationshipRepository extends  GraphRepository<NeoRelationship>{	
}
