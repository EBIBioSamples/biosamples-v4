package uk.ac.ebi.biosamples.mongo.repo;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import uk.ac.ebi.biosamples.mongo.model.MongoRelationship;

public interface MongoRelationshipRepository extends MongoRepository<MongoRelationship, String> {
	
	List<MongoRelationship> findAllByTarget(String target);
	List<MongoRelationship> findAllBySource(String source);
}
