package uk.ac.ebi.biosamples.repos;

import org.springframework.data.neo4j.repository.GraphRepository;

import uk.ac.ebi.biosamples.models.NeoRelationship;

public interface NeoRelationshipRepository extends  GraphRepository<NeoRelationship>{	
}
