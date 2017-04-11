package uk.ac.ebi.biosamples.neo.service;

import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Service;

import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.neo.model.NeoAttribute;
import uk.ac.ebi.biosamples.neo.model.NeoSample;

@Service
@ConfigurationPropertiesBinding
public class NeoAttributeToAttributeConverter
		implements Converter<NeoAttribute, Attribute> {

	@Override
	public Attribute convert(NeoAttribute neo) {
		throw new IllegalArgumentException("Not implemented");
		
	}

}
