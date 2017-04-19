package uk.ac.ebi.biosamples.neo.service.modelconverter;

import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Service;

import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.ExternalReference;
import uk.ac.ebi.biosamples.model.Relationship;
import uk.ac.ebi.biosamples.neo.model.NeoAttribute;
import uk.ac.ebi.biosamples.neo.model.NeoExternalReference;
import uk.ac.ebi.biosamples.neo.model.NeoRelationship;
import uk.ac.ebi.biosamples.neo.model.NeoSample;

@Service
@ConfigurationPropertiesBinding
public class RelationshipToNeoRelationshipConverter
		implements Converter<Relationship, NeoRelationship> {

	@Override
	public NeoRelationship convert(Relationship relationship) {
		
		NeoSample owner = NeoSample.create(relationship.getSource());
		NeoSample target = NeoSample.create(relationship.getTarget());
		
		return NeoRelationship.build(owner, relationship.getType(), target);
	}

}
