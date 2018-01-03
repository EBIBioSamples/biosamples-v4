package uk.ac.ebi.biosamples.service;

import java.util.Collection;

import org.springframework.stereotype.Service;

import uk.ac.ebi.biosamples.model.Attribute;

@Service
public class AttributeValidator  {

	public void validate(Attribute attribute, Collection<String> errors) {
		/*
		if (attribute.getType().length() > 255) {
			errors.add(attribute+" type too long");
		}
		if (attribute.getValue().length() > 255) {
			errors.add(attribute+" value too long");
		}
		if (attribute.getIri() != null && attribute.getIri().length() > 255) {
			errors.add(attribute+" iri too long");
		}
		if (attribute.getUnit() != null && attribute.getUnit().length() > 255) {
			errors.add(attribute+" unit too long");
		}
		*/
	}

}
