package uk.ac.ebi.biosamples.neo.service.modelconverter;

import java.util.ArrayList;
import java.util.Collection;

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
public class SampleToNeoSampleConverter
		implements Converter<Sample, NeoSample> {
	
	public SampleToNeoSampleConverter() {
		
	}

	@Override
	public NeoSample convert(Sample sample) {
		Collection<NeoAttribute> neoAttributes = new ArrayList<>();
		Collection<NeoExternalReference> neoExternalReferences = new ArrayList<>();
		
		for (Attribute attribute : sample.getCharacteristics()) {
			neoAttributes.add(NeoAttribute.build(attribute.getType(), attribute.getValue(), attribute.getIri(), attribute.getUnit()));
		}
		for (ExternalReference externalReference : sample.getExternalReferences()) {
			neoExternalReferences.add(NeoExternalReference.build(externalReference.getUrl()));
		}	
		
		NeoSample neoSample = NeoSample.build(sample.getName(), sample.getAccession(), sample.getRelease(), sample.getUpdate(),
				neoAttributes, null, neoExternalReferences);

		for (Relationship relationship : sample.getRelationships()) {

			NeoSample owner;
			NeoSample target;
			
			if (relationship.getSource().equals(sample.getAccession())) {
				owner = neoSample;
			} else {
				owner = NeoSample.create(relationship.getSource());
			}
			if (relationship.getTarget().equals(sample.getAccession())) {
				target = neoSample;
			} else {
				target = NeoSample.create(relationship.getTarget());
			}
						
			neoSample.getRelationships().add(NeoRelationship.build(owner, relationship.getType(), target));
		}			
		
		return neoSample;	
	}

}
