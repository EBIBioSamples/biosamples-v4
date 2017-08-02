package uk.ac.ebi.biosamples.mongo.service;

import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Service;

import uk.ac.ebi.biosamples.model.Curation;
import uk.ac.ebi.biosamples.model.ExternalReference;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.mongo.model.MongoCuration;
import uk.ac.ebi.biosamples.mongo.model.MongoExternalReference;
import uk.ac.ebi.biosamples.mongo.model.MongoSample;

@Service
@ConfigurationPropertiesBinding
public class MongoExternalReferenceToExternalReferenceConverter implements Converter<MongoExternalReference, ExternalReference> {

	@Override
	public ExternalReference convert(MongoExternalReference externalReference) {
		return ExternalReference.build(externalReference.getUrl());
	}
}
