package uk.ac.ebi.biosamples.mongo.service;

import java.util.SortedSet;
import java.util.TreeSet;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Service;

import uk.ac.ebi.biosamples.model.ExternalReference;
import uk.ac.ebi.biosamples.model.Relationship;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.mongo.model.MongoExternalReference;
import uk.ac.ebi.biosamples.mongo.model.MongoRelationship;
import uk.ac.ebi.biosamples.mongo.model.MongoSample;

@Service
public class SampleToMongoSampleConverter implements Converter<Sample, MongoSample> {

	@Autowired
	private ExternalReferenceToMongoExternalReferenceConverter externalReferenceToMongoExternalReferenceConverter;
	@Autowired
	private RelationshipToMongoRelationshipConverter relationshipToMongoRelationshipConverter;

	@Override
	public MongoSample convert(Sample sample) {

		SortedSet<MongoExternalReference> externalReferences = new TreeSet<>();
		for (ExternalReference mongoExternalReference : sample.getExternalReferences()) {
			externalReferences.add(externalReferenceToMongoExternalReferenceConverter.convert(mongoExternalReference));
		}
		
		SortedSet<MongoRelationship> relationships = new TreeSet<>();
		for (Relationship relationship : sample.getRelationships()) {
			relationships.add(relationshipToMongoRelationshipConverter.convert(relationship));
		}
		
		//when we convert to a MongoSample then the Sample *must* have a domain
		if (sample.getDomain() == null) {
			throw new RuntimeException("sample does not have domain "+sample);
		}
		
		return MongoSample.build(sample.getName(), sample.getAccession(), sample.getDomain(), 
				sample.getRelease(), sample.getUpdate(),
				sample.getCharacteristics(), relationships, externalReferences, 
				sample.getOrganizations(), sample.getContacts());
	}
}
