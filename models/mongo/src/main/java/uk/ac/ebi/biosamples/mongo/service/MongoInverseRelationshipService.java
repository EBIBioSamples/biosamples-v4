package uk.ac.ebi.biosamples.mongo.service;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.BasicQuery;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import uk.ac.ebi.biosamples.mongo.model.MongoRelationship;
import uk.ac.ebi.biosamples.mongo.model.MongoSample;

@Service
public class MongoInverseRelationshipService {

	private final MongoTemplate mongoTemplate;
	
	public MongoInverseRelationshipService(MongoTemplate mongoTemplate) {
		this.mongoTemplate = mongoTemplate;
	}
	
	public MongoSample addInverseRelationships(MongoSample mongoSample) {
		String accession = mongoSample.getAccession();
		if (accession == null) {
			return mongoSample;
		}
		
		Query query = new BasicQuery("{'relationships.target':'"+accession+"'}","{'relationships.$':1}");
		for (MongoSample other : mongoTemplate.find(query, MongoSample.class)) {
			for (MongoRelationship relationship : other.getRelationships()) {
				if (relationship.getTarget().equals(accession)) {
					mongoSample.getRelationships().add(relationship);
				}
			}
		}
		return mongoSample;
	}
}
