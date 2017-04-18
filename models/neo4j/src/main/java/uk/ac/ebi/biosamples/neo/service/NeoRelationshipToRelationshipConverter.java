package uk.ac.ebi.biosamples.neo.service;

import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Service;

import uk.ac.ebi.biosamples.model.Relationship;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.neo.model.NeoRelationship;
import uk.ac.ebi.biosamples.neo.model.NeoSample;

@Service
@ConfigurationPropertiesBinding
public class NeoRelationshipToRelationshipConverter
		implements Converter<NeoRelationship, Relationship> {

	@Override
	public Relationship convert(NeoRelationship neo) {
		return Relationship.build(neo.getOwner().getAccession(), neo.getType(), neo.getTarget().getAccession());
	}

}
