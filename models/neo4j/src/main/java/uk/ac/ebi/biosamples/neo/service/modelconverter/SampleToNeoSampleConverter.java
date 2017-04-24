package uk.ac.ebi.biosamples.neo.service.modelconverter;

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
import uk.ac.ebi.biosamples.neo.model.NeoExternalReferenceApplication;

@Service
@ConfigurationPropertiesBinding
public class SampleToNeoSampleConverter
		implements Converter<Sample, NeoSample> {
	
	private AttributeToNeoAttributeConverter attributeToNeoAttributeConverter;
	private ExternalReferenceToNeoExternalReferenceConverter externalReferenceToNeoExternalReferenceConverter;
	private RelationshipToNeoRelationshipConverter relationshipToNeoRelationshipConverter;

	public SampleToNeoSampleConverter( AttributeToNeoAttributeConverter attributeToNeoAttributeConverter,
			ExternalReferenceToNeoExternalReferenceConverter externalReferenceToNeoExternalReferenceConverter,
			RelationshipToNeoRelationshipConverter relationshipToNeoRelationshipConverter) {
		
		this.attributeToNeoAttributeConverter = attributeToNeoAttributeConverter;
		this.externalReferenceToNeoExternalReferenceConverter = externalReferenceToNeoExternalReferenceConverter;
		this.relationshipToNeoRelationshipConverter = relationshipToNeoRelationshipConverter;
	}

	@Override
	public NeoSample convert(Sample sample) {
		NeoSample neoSample = NeoSample.build(sample.getName(), sample.getAccession(), sample.getRelease(), sample.getUpdate(),
				null, null, null);
		for (Attribute attribute : sample.getCharacteristics()) {
			neoSample.getAttributes().add(attributeToNeoAttributeConverter.convert(attribute));
		}
		for (ExternalReference externalReference : sample.getExternalReferences()) {
			NeoExternalReference neoExternalReference = externalReferenceToNeoExternalReferenceConverter.convert(externalReference);
			NeoExternalReferenceApplication neoExternalReferenceApplication = NeoExternalReferenceApplication.build(neoSample, neoExternalReference);
			neoSample.getExternalReferenceApplications().add(neoExternalReferenceApplication);
		}
		for (Relationship relationship : sample.getRelationships()) {
			neoSample.getRelationships().add(relationshipToNeoRelationshipConverter.convert(relationship));
		}				
		return neoSample;	
	}

}
