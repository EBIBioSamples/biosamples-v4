package uk.ac.ebi.biosamples.neo.service;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Service;

import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.ExternalReference;
import uk.ac.ebi.biosamples.model.Relationship;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.neo.model.NeoRelationship;
import uk.ac.ebi.biosamples.neo.model.NeoSample;
import uk.ac.ebi.biosamples.neo.model.NeoAttribute;
import uk.ac.ebi.biosamples.neo.model.NeoExternalReference;

@Service
@ConfigurationPropertiesBinding
public class SampleToNeoSampleConverter
		implements Converter<Sample, NeoSample> {
	
	//@Autowired
	//private ConversionService conversionService;
	
	@Autowired
	private AttributeToNeoAttributeConverter attributeToNeoAttributeConverter;
	@Autowired
	private ExternalReferenceToNeoExternalReferenceConverter externalReferenceToNeoExternalReferenceConverter;
	@Autowired
	private RelationshipToNeoRelationshipConverter relationshipToNeoRelationshipConverter;

	@Override
	public NeoSample convert(Sample sample) {
		
		Set<NeoAttribute> attributes = new HashSet<>();
		for (Attribute attribute : sample.getAttributes()) {
			attributes.add(attributeToNeoAttributeConverter.convert(attribute));
		}
		Set<NeoExternalReference> externalReferences = new HashSet<>();
		for (ExternalReference externalReference : sample.getExternalReferences()) {
			externalReferences.add(externalReferenceToNeoExternalReferenceConverter.convert(externalReference));
		}
		Set<NeoRelationship> relationships = new HashSet<>();
		for (Relationship relationship : sample.getRelationships()) {
			relationships.add(relationshipToNeoRelationshipConverter.convert(relationship));
		}				
		return NeoSample.build(sample.getName(), sample.getAccession(), sample.getRelease(), sample.getUpdate(),
				attributes, relationships, externalReferences);
	
	}

}
