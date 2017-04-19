package uk.ac.ebi.biosamples.neo.service;

import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Service;

import uk.ac.ebi.biosamples.model.ExternalReference;
import uk.ac.ebi.biosamples.neo.model.NeoExternalReference;

@Service
@ConfigurationPropertiesBinding
public class ExternalReferenceToNeoExternalReferenceConverter
		implements Converter<ExternalReference, NeoExternalReference> {

	@Override
	public NeoExternalReference convert(ExternalReference externalReference) {
		return NeoExternalReference.build(externalReference.getUrl());
	}

}
