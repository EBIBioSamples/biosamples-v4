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
import uk.ac.ebi.biosamples.neo.model.NeoExternalReferenceLink;

@Service
@ConfigurationPropertiesBinding
public class SampleToNeoSampleConverter
		implements Converter<Sample, NeoSample> {
	
	public SampleToNeoSampleConverter() {
		
	}

	@Override
	public NeoSample convert(Sample sample) {
		NeoSample neoSample = NeoSample.build(sample.getName(), sample.getAccession(), sample.getRelease(), sample.getUpdate(),
				null, null, null);
		for (Attribute attribute : sample.getCharacteristics()) {
			neoSample.getAttributes().add(NeoAttribute.build(attribute.getType(), attribute.getValue(), attribute.getIri(), attribute.getUnit()));
		}
		for (ExternalReference externalReference : sample.getExternalReferences()) {
			//create the external reference node
			NeoExternalReference neoExternalReference = NeoExternalReference.build(externalReference.getUrl());
			//build the link from both ends
			NeoExternalReferenceLink neoExternalReferenceApplication = NeoExternalReferenceLink.build(neoSample, neoExternalReference);
			//then add the link back to the ends
			neoSample.getExternalReferenceLinks().add(neoExternalReferenceApplication);
			neoExternalReference.getLinks().add(neoExternalReferenceApplication);
		}
		for (Relationship relationship : sample.getRelationships()) {

			NeoSample owner = NeoSample.create(relationship.getSource());
			NeoSample target = NeoSample.create(relationship.getTarget());
			
			if (owner.getAccession().equals(sample.getAccession())) {
				owner = neoSample;
			}
			if (target.getAccession().equals(sample.getAccession())) {
				target = neoSample;
			}
						
			neoSample.getRelationships().add(NeoRelationship.build(owner, relationship.getType(), target));
		}				
		return neoSample;	
	}

}
