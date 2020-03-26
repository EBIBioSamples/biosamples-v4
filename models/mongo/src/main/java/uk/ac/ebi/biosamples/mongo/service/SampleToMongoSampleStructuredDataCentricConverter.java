package uk.ac.ebi.biosamples.mongo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

/**
 * Class to convert {@link Sample} to {@link MongoSample} without validating if sample has a domain.
 * This is useful for structured data PUT requests where we don't enforce user to have a domain in the {@link Sample}
 */
@Service
public class SampleToMongoSampleStructuredDataCentricConverter implements Converter<Sample, MongoSample> {
    @Autowired
    private ExternalReferenceToMongoExternalReferenceConverter externalReferenceToMongoExternalReferenceConverter;
    @Autowired
    private RelationshipToMongoRelationshipConverter relationshipToMongoRelationshipConverter;

    private static Logger LOGGER = LoggerFactory.getLogger(MongoSampleToSampleConverter.class);

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

        sample.getData().forEach(data -> {
            if (data.getDomain() == null || data.getDomain().isEmpty()) {
                LOGGER.warn(String.format("sample %s structured data does not have a domain", sample.getAccession()));
                throw new RuntimeException("sample structured data does not have domain " + sample);
            }
        });

        return MongoSample.build(sample.getName(), sample.getAccession(), sample.getDomain(),
                sample.getRelease(), sample.getUpdate(), sample.getCreate(),
                sample.getCharacteristics(), sample.getData(), relationships, externalReferences,
                sample.getOrganizations(), sample.getContacts(), sample.getPublications(), sample.getSubmittedVia());
    }
}
