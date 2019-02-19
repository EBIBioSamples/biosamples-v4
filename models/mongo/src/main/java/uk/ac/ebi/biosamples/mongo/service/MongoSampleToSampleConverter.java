package uk.ac.ebi.biosamples.mongo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.model.ExternalReference;
import uk.ac.ebi.biosamples.model.Relationship;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.mongo.model.MongoExternalReference;
import uk.ac.ebi.biosamples.mongo.model.MongoRelationship;
import uk.ac.ebi.biosamples.mongo.model.MongoSample;

import java.util.SortedSet;
import java.util.TreeSet;

@Service
public class MongoSampleToSampleConverter implements Converter<MongoSample, Sample> {

	@Autowired
	private MongoExternalReferenceToExternalReferenceConverter mongoExternalReferenceToExternalReferenceConverter;
	@Autowired
	private MongoRelationshipToRelationshipConverter mongoRelationshipToRelationshipConverter;

	@Override
	public Sample convert(MongoSample sample) {

		SortedSet<ExternalReference> externalReferences = new TreeSet<>();
		for (MongoExternalReference mongoExternalReference : sample.getExternalReferences()) {
			externalReferences.add(mongoExternalReferenceToExternalReferenceConverter.convert(mongoExternalReference));
		}

		SortedSet<Relationship> relationships = new TreeSet<>();
		for (MongoRelationship mongoRelationship : sample.getRelationships()) {
			relationships.add(mongoRelationshipToRelationshipConverter.convert(mongoRelationship));
		}

		//when we convert to a MongoSample then the Sample *must* have a domain
		if (sample.getDomain() == null) {
			throw new RuntimeException("sample does not have domain " + sample);
		}

		return new Sample.Builder(sample.getName(), sample.getAccession()).withDomain(sample.getDomain())
				.withRelease(sample.getRelease()).withUpdate(sample.getUpdate())
				.withAttributes(sample.getAttributes()).withRelationships(relationships)
                .withData(sample.getData())
				.withExternalReferences(externalReferences).withOrganizations(sample.getOrganizations())
				.withContacts(sample.getContacts()).withPublications(sample.getPublications())
				.build();
	}
}
