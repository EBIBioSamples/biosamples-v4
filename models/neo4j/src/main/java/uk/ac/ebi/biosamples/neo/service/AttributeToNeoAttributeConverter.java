package uk.ac.ebi.biosamples.neo.service;

import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Service;

import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.neo.model.NeoAttribute;

@Service
@ConfigurationPropertiesBinding
public class AttributeToNeoAttributeConverter
		implements Converter<Attribute, NeoAttribute> {

	@Override
	public NeoAttribute convert(Attribute attribute) {
		return NeoAttribute.build(attribute.getType(), attribute.getValue(), attribute.getIri(), attribute.getUnit());
	}

}
