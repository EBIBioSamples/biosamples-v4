package uk.ac.ebi.biosamples.neo.service.modelconverter;

import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Service;

import uk.ac.ebi.biosamples.model.ExternalReference;
import uk.ac.ebi.biosamples.neo.model.NeoExternalReference;

@Service
@ConfigurationPropertiesBinding
public class NeoExternalReferenceToExternalReferenceConverter
		implements Converter<NeoExternalReference, ExternalReference> {

	@Override
	public ExternalReference convert(NeoExternalReference neo) {
		if (neo == null) return null;
		return ExternalReference.build(neo.getUrl());
		
	}

}
