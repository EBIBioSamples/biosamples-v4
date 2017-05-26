package uk.ac.ebi.biosamples.neo.service.modelconverter;

import java.util.Set;
import java.util.TreeSet;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Service;

import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.Curation;
import uk.ac.ebi.biosamples.model.ExternalReference;
import uk.ac.ebi.biosamples.neo.model.NeoAttribute;
import uk.ac.ebi.biosamples.neo.model.NeoCuration;
import uk.ac.ebi.biosamples.neo.model.NeoExternalReference;

@Service
public class NeoCurationToCurationConverter
		implements Converter<NeoCuration, Curation> {

	@Override
	public Curation convert(NeoCuration neo) {		
		Set<Attribute> preAttributes = new TreeSet<>();
		Set<Attribute> postAttributes = new TreeSet<>();
		Set<ExternalReference> preExternals = new TreeSet<>();
		Set<ExternalReference> postExternals = new TreeSet<>();
		
		if (neo.getAttributesPre() != null) {
			for (NeoAttribute neoAttribute : neo.getAttributesPre()) {
				Attribute attribute = Attribute.build(neoAttribute.getType(), 
						neoAttribute.getValue(), neoAttribute.getIri(), neoAttribute.getUnit());
				preAttributes.add(attribute);
			}
		}
		if (neo.getAttributesPost() != null) {
			for (NeoAttribute neoAttribute : neo.getAttributesPost()) {
				Attribute attribute = Attribute.build(neoAttribute.getType(), 
						neoAttribute.getValue(), neoAttribute.getIri(), neoAttribute.getUnit());
				postAttributes.add(attribute);
			}
		}

		if (neo.getExternalsPre() != null) {
			for (NeoExternalReference neoExternal : neo.getExternalsPre()) {
				ExternalReference external = ExternalReference.build(neoExternal.getUrl());
				preExternals.add(external);
			}
		}
		if (neo.getExternalsPost() != null) {
			for (NeoExternalReference neoExternal : neo.getExternalsPost()) {
				ExternalReference external = ExternalReference.build(neoExternal.getUrl());
				postExternals.add(external);
			}
		}
		
		return Curation.build(preAttributes, postAttributes, preExternals, postExternals);
		
	}

}
