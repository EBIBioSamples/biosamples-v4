package uk.ac.ebi.biosamples.mongo.service;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Service;

import uk.ac.ebi.biosamples.model.Relationship;
import uk.ac.ebi.biosamples.mongo.model.MongoRelationship;

@Service
public class MongoRelationshipToRelationshipConverter implements Converter<MongoRelationship, Relationship>{
	@Override
	public Relationship convert(MongoRelationship relationship) {
		return Relationship.build(relationship.getSource(), relationship.getType(), relationship.getTarget());
	}
}
