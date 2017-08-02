package uk.ac.ebi.biosamples.mongo.service;

import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Service;

import uk.ac.ebi.biosamples.model.Curation;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.mongo.model.MongoCuration;
import uk.ac.ebi.biosamples.mongo.model.MongoSample;

@Service
@ConfigurationPropertiesBinding
public class MongoCurationToCurationConverter implements Converter<MongoCuration, Curation> {

	@Override
	public Curation convert(MongoCuration mongoCuration) {
		return Curation.build(mongoCuration.getAttributesPre(), mongoCuration.getAttributesPost(), 
				mongoCuration.getExternalReferencesPre(), mongoCuration.getExternalReferencesPost());
	}
}
