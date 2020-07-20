package uk.ac.ebi.biosamples.mongo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.model.Certificate;
import uk.ac.ebi.biosamples.model.ExternalReference;
import uk.ac.ebi.biosamples.model.Relationship;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.mongo.model.MongoCertificate;
import uk.ac.ebi.biosamples.mongo.model.MongoExternalReference;
import uk.ac.ebi.biosamples.mongo.model.MongoRelationship;
import uk.ac.ebi.biosamples.mongo.model.MongoSample;

import java.util.SortedSet;
import java.util.TreeSet;

@Service
public class SampleToMongoSampleConverter implements Converter<Sample, MongoSample> {
	@Autowired
	private ExternalReferenceToMongoExternalReferenceConverter externalReferenceToMongoExternalReferenceConverter;
	@Autowired
	private RelationshipToMongoRelationshipConverter relationshipToMongoRelationshipConverter;
	@Autowired
	private CertificateToMongoCertificateConverter certificateToMongoCertificateConverter;

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

		SortedSet<MongoCertificate> certificates = new TreeSet<>();

		for (Certificate certificate : sample.getCertificates()) {
			certificates.add(certificateToMongoCertificateConverter.convert(certificate));
		}
		
		//when we convert to a MongoSample then the Sample *must* have a domain
		if (sample.getDomain() == null) {
			throw new RuntimeException("sample does not have domain "+sample);
		}
		
		return MongoSample.build(sample.getName(), sample.getAccession(), sample.getDomain(), 
				sample.getRelease(), sample.getUpdate(), sample.getCreate(),
				sample.getCharacteristics(), sample.getData(), relationships, externalReferences,
				sample.getOrganizations(), sample.getContacts(), sample.getPublications(), certificates,
				sample.getSubmittedVia());
	}
}
