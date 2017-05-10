package uk.ac.ebi.biosamples.neo.service.modelconverter;

import java.util.HashSet;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Service;

import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.Curation;
import uk.ac.ebi.biosamples.neo.model.NeoAttribute;
import uk.ac.ebi.biosamples.neo.model.NeoCuration;

@Service
@ConfigurationPropertiesBinding
public class NeoCurationToCurationConverter
		implements Converter<NeoCuration, Curation> {

	@Override
	public Curation convert(NeoCuration neo) {		
		Set<Attribute> preAttributes = new HashSet<>();
		Set<Attribute> postAttributes = new HashSet<>();
		
		for (NeoAttribute neoAttribute : neo.getAttributesPre()) {
			Attribute attribute = Attribute.build(neoAttribute.getType(), 
					neoAttribute.getValue(), neoAttribute.getIri(), neoAttribute.getUnit());
			preAttributes.add(attribute);
		}

		for (NeoAttribute neoAttribute : neo.getAttributesPost()) {
			Attribute attribute = Attribute.build(neoAttribute.getType(), 
					neoAttribute.getValue(), neoAttribute.getIri(), neoAttribute.getUnit());
			postAttributes.add(attribute);
		}
		
		return Curation.build(preAttributes, postAttributes);
		
	}

}
