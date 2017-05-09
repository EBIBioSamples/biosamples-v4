package uk.ac.ebi.biosamples.neo.service.modelconverter;

import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Service;

import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.Curation;
import uk.ac.ebi.biosamples.neo.model.NeoAttribute;
import uk.ac.ebi.biosamples.neo.model.NeoCuration;

@Service
@ConfigurationPropertiesBinding
public class CurationToNeoCurationConverter
		implements Converter<Curation, NeoCuration> {

	@Override
	public NeoCuration convert(Curation curation) {		
		Set<NeoAttribute> preAttributes = new HashSet<>();
		Set<NeoAttribute> postAttributes = new HashSet<>();
		
		for (Attribute attribute : curation.getAttributesPre()) {
			NeoAttribute neoAttribute = NeoAttribute.build(attribute.getType(), 
					attribute.getValue(), attribute.getIri(), attribute.getUnit());
			preAttributes.add(neoAttribute);
		}

		for (Attribute attribute : curation.getAttributesPost()) {
			NeoAttribute neoAttribute = NeoAttribute.build(attribute.getType(), 
					attribute.getValue(), attribute.getIri(), attribute.getUnit());
			postAttributes.add(neoAttribute);
		}
		
		return NeoCuration.build(preAttributes, postAttributes);
		
	}

}
