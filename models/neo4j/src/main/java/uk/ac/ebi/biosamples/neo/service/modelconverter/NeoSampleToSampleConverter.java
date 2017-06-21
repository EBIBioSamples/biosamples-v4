package uk.ac.ebi.biosamples.neo.service.modelconverter;

import java.util.HashSet;
import java.util.Set;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Service;

import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.ExternalReference;
import uk.ac.ebi.biosamples.model.Relationship;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.neo.model.NeoAttribute;
import uk.ac.ebi.biosamples.neo.model.NeoExternalReference;
import uk.ac.ebi.biosamples.neo.model.NeoRelationship;
import uk.ac.ebi.biosamples.neo.model.NeoSample;

@Service
public class NeoSampleToSampleConverter
		implements Converter<NeoSample, Sample> {
	
	
	public NeoSampleToSampleConverter() {
	}
	
	/**
	 * This will load associated objects as necessary.
	 * 
	 * 
	 */
	@Override
	public Sample convert(NeoSample neo) {		
		
		
		Set<Attribute> attributes = new HashSet<>();
		if (neo.getAttributes() != null) {
			for (NeoAttribute attribute : neo.getAttributes()) {
				attributes.add(Attribute.build(attribute.getType(), attribute.getValue(), attribute.getIri(), attribute.getUnit()));
			}
		}
		Set<ExternalReference> externalReferences = new HashSet<>();
		if (neo.getExternalReferences() != null) {
			for (NeoExternalReference neoExternalReference : neo.getExternalReferences()) {
				externalReferences.add(ExternalReference.build(neoExternalReference.getUrl()));
			}
		}
		Set<Relationship> relationships = new HashSet<>();
		if (neo.getRelationships() != null) {
			for (NeoRelationship relationship : neo.getRelationships()) {
				relationships.add(Relationship.build(relationship.getOwner().getAccession(), relationship.getType(), relationship.getTarget().getAccession()));
			}				
		}
		return Sample.build(neo.getName(), neo.getAccession(), neo.getDomain(), neo.getRelease(), neo.getUpdate(),
				attributes, relationships, externalReferences);
	}

}
