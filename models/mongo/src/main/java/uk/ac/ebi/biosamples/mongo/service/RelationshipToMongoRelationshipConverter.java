package uk.ac.ebi.biosamples.mongo.service;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Service;

import uk.ac.ebi.biosamples.model.Relationship;
import uk.ac.ebi.biosamples.mongo.model.MongoRelationship;

@Service
public class RelationshipToMongoRelationshipConverter implements Converter<Relationship, MongoRelationship>{

	@Override
	public MongoRelationship convert(Relationship relationship) {
		return MongoRelationship.build(relationship.getSource(), relationship.getType(), relationship.getTarget());
	}

}
