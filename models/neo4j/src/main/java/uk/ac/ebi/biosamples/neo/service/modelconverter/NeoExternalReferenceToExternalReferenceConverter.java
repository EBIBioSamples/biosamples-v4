package uk.ac.ebi.biosamples.neo.service.modelconverter;

import java.util.HashSet;
import java.util.Set;

import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Service;

import uk.ac.ebi.biosamples.model.ExternalReference;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.neo.model.NeoExternalReference;
import uk.ac.ebi.biosamples.neo.model.NeoExternalReferenceApplication;
import uk.ac.ebi.biosamples.neo.model.NeoSample;

@Service
@ConfigurationPropertiesBinding
public class NeoExternalReferenceToExternalReferenceConverter
		implements Converter<NeoExternalReference, ExternalReference> {

	@Override
	public ExternalReference convert(NeoExternalReference neo) {
		Set<String> samples = new HashSet<>();
		for (NeoExternalReferenceApplication application : neo.getApplications()) {
			samples.add(application.getSample().getAccession());
		}
		return ExternalReference.build(neo.getUrl());
		
	}

}
