package uk.ac.ebi.biosamples.neo.service;

import java.net.URI;

import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Service;

import uk.ac.ebi.biosamples.model.ExternalReference;
import uk.ac.ebi.biosamples.model.Relationship;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.neo.model.NeoRelationship;
import uk.ac.ebi.biosamples.neo.model.NeoSample;
import uk.ac.ebi.biosamples.neo.model.NeoExternalReference;

@Service
@ConfigurationPropertiesBinding
public class SampleToNeoSampleConverter
		implements Converter<Sample, NeoSample> {

	@Override
	public NeoSample convert(Sample sample) {
		NeoSample neoSample = new NeoSample(sample.getAccession());
		if (sample.getRelationships() != null && sample.getRelationships().size() > 0) {
			for(Relationship relationship : sample.getRelationships()) {
				NeoSample neoSampleTarget = new NeoSample(relationship.getTarget());
				NeoRelationship neoRelationship = NeoRelationship.create(neoSample, neoSampleTarget, relationship.getType());
				neoSample.addRelationships(neoRelationship);
			}
		}
		
		if (sample.getExternalReferences() != null && sample.getExternalReferences().size() > 0) {
			for (ExternalReference externalReference : sample.getExternalReferences()) {
				neoSample.addExternalReference(NeoExternalReference.create(externalReference.getUrl()));
			}
		}
		
		return neoSample;		
	}

}
