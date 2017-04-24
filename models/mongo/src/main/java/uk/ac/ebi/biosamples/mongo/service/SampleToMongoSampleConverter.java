package uk.ac.ebi.biosamples.mongo.service;

import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Service;

import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.mongo.model.MongoSample;

@Service
@ConfigurationPropertiesBinding
public class SampleToMongoSampleConverter implements Converter<Sample, MongoSample> {

	@Override
	public MongoSample convert(Sample sample) {
		return MongoSample.build(sample.getName(), sample.getAccession(), sample.getRelease(), sample.getUpdate(),
				sample.getCharacteristics(), sample.getRelationships(), sample.getExternalReferences());
	}
}
