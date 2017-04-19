package uk.ac.ebi.biosamples.neo.service;

import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
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
@ConfigurationPropertiesBinding
public class NeoSampleToSampleConverter
		implements Converter<NeoSample, Sample> {
	
	@Autowired
	private NeoAttributeToAttributeConverter neoAttributeToAttributeConverter;
	@Autowired
	private NeoExternalReferenceToExternalReferenceConverter neoExternalReferenceToExternalReferenceConverter;
	@Autowired
	private NeoRelationshipToRelationshipConverter neoRelationshipToRelationshipConverter;

	@Override
	public Sample convert(NeoSample neo) {		
		Set<Attribute> attributes = new HashSet<>();
		if (neo.getAttributes() != null) {
			for (NeoAttribute attribute : neo.getAttributes()) {
				attributes.add(neoAttributeToAttributeConverter.convert(attribute));
			}
		}
		Set<ExternalReference> externalReferences = new HashSet<>();
		if (neo.getExternalReferences() != null) {
			for (NeoExternalReference externalReference : neo.getExternalReferences()) {
				externalReferences.add(neoExternalReferenceToExternalReferenceConverter.convert(externalReference));
			}
		}
		Set<Relationship> relationships = new HashSet<>();
		if (neo.getRelationships() != null) {
			for (NeoRelationship relationship : neo.getRelationships()) {
				relationships.add(neoRelationshipToRelationshipConverter.convert(relationship));
			}				
		}
		return Sample.build(neo.getName(), neo.getAccession(), neo.getRelease(), neo.getUpdate(),
				attributes, relationships, externalReferences);
	}

}
