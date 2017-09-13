package uk.ac.ebi.biosamples.mongo.service;

import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Service;

import uk.ac.ebi.biosamples.model.ExternalReference;
import uk.ac.ebi.biosamples.mongo.model.MongoExternalReference;

@Service
@ConfigurationPropertiesBinding
public class ExternalReferenceToMongoExternalReferenceConverter implements Converter<ExternalReference, MongoExternalReference> {

	@Override
	public MongoExternalReference convert(ExternalReference externalReference) {
		return MongoExternalReference.build(externalReference.getUrl());
	}
}
