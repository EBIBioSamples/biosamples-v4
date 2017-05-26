package uk.ac.ebi.biosamples.neo.service.modelconverter;

import java.util.ArrayList;
import java.util.Collection;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Service;

import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.Curation;
import uk.ac.ebi.biosamples.model.ExternalReference;
import uk.ac.ebi.biosamples.neo.model.NeoAttribute;
import uk.ac.ebi.biosamples.neo.model.NeoCuration;
import uk.ac.ebi.biosamples.neo.model.NeoExternalReference;

@Service
public class CurationToNeoCurationConverter implements Converter<Curation, NeoCuration> {

	@Override
	public NeoCuration convert(Curation curation) {	

		Collection<NeoAttribute> attributesPre = new ArrayList<>();
		Collection<NeoAttribute> attributesPost = new ArrayList<>();
		Collection<NeoExternalReference> externalsPre = new ArrayList<>();
		Collection<NeoExternalReference> externalsPost = new ArrayList<>();
		
		for (Attribute attribute : curation.getAttributesPre()) {
			attributesPre.add(NeoAttribute.build(attribute.getType(), attribute.getValue(), attribute.getIri(), attribute.getUnit()));
		}
		for (Attribute attribute : curation.getAttributesPost()) {
			attributesPost.add(NeoAttribute.build(attribute.getType(), attribute.getValue(), attribute.getIri(), attribute.getUnit()));
		}
		for (ExternalReference external: curation.getExternalReferencesPre()) {
			externalsPre.add(NeoExternalReference.build(external.getUrl()));
		}
		for (ExternalReference external: curation.getExternalReferencesPost()) {
			externalsPost.add(NeoExternalReference.build(external.getUrl()));			
		}
		
		NeoCuration neoCuration = NeoCuration.build(attributesPre, attributesPost, externalsPre, externalsPost);
		
		return neoCuration;
		
	}

}
