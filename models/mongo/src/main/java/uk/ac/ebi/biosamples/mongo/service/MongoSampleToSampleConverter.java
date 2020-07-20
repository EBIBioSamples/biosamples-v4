package uk.ac.ebi.biosamples.mongo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
public class MongoSampleToSampleConverter implements Converter<MongoSample, Sample> {
    @Autowired
    private MongoExternalReferenceToExternalReferenceConverter mongoExternalReferenceToExternalReferenceConverter;
    @Autowired
    private MongoRelationshipToRelationshipConverter mongoRelationshipToRelationshipConverter;
    @Autowired
    private MongoCertificateToCertificateConverter mongoCertificateToCertificateConverter;

    private static Logger LOGGER = LoggerFactory.getLogger(MongoSampleToSampleConverter.class);

    @Override
    public Sample convert(MongoSample sample) {
        final SortedSet<ExternalReference> externalReferences = new TreeSet<>();

        if (sample.getExternalReferences() != null && sample.getExternalReferences().size() > 0) {
            for (final MongoExternalReference mongoExternalReference : sample.getExternalReferences()) {
                if (mongoExternalReference != null)
                    externalReferences.add(mongoExternalReferenceToExternalReferenceConverter.convert(mongoExternalReference));
            }
        }

        final SortedSet<Relationship> relationships = new TreeSet<>();

        if (sample.getRelationships() != null && sample.getRelationships().size() > 0) {
            for (final MongoRelationship mongoRelationship : sample.getRelationships()) {
                if (mongoRelationship != null)
                    relationships.add(mongoRelationshipToRelationshipConverter.convert(mongoRelationship));
            }
        }

        SortedSet<Certificate> certificates = new TreeSet<>();

        if (sample.getCertificates() != null && sample.getCertificates().size() > 0) {
            for (MongoCertificate certificate : sample.getCertificates()) {
                if (certificate != null) {
                    certificates.add(mongoCertificateToCertificateConverter.convert(certificate));
                }
            }
        }

        //when we convert to a MongoSample then the Sample *must* have a domain
        if (sample.getDomain() == null) {
            LOGGER.warn(String.format("sample %s does not have a domain", sample.getAccession()));
            throw new RuntimeException("sample does not have domain " + sample);
        }

        return new Sample.Builder(sample.getName(), sample.getAccession()).withDomain(sample.getDomain())
                .withRelease(sample.getRelease()).withUpdate(sample.getUpdate()).withCreate(sample.getCreate())
                .withAttributes(sample.getAttributes()).withRelationships(relationships)
                .withData(sample.getData())
                .withExternalReferences(externalReferences).withOrganizations(sample.getOrganizations())
                .withContacts(sample.getContacts()).withPublications(sample.getPublications()).withCertificates(certificates)
                .build();
    }
}
