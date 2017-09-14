package uk.ac.ebi.biosamples.mongo.service;

import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Service;

import uk.ac.ebi.biosamples.model.Curation;
import uk.ac.ebi.biosamples.mongo.model.MongoCuration;

@Service
@ConfigurationPropertiesBinding
public class CurationToMongoCurationConverter implements Converter<Curation, MongoCuration> {

	@Override
	public MongoCuration convert(Curation curation) {
		return MongoCuration.build(curation.getAttributesPre(), curation.getAttributesPost(), 
				curation.getExternalReferencesPre(), curation.getExternalReferencesPost());
	}
}
