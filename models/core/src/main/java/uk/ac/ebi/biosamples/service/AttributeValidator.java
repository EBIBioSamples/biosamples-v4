package uk.ac.ebi.biosamples.service;

import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.Sample;

@Service
public class AttributeValidator implements Validator {

	@Override
	public boolean supports(Class<?> clazz) {
		return Attribute.class.isAssignableFrom(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		Attribute attribute = (Attribute) target;
		if (attribute.getType().length() > 255) {
			errors.rejectValue("type", "too.long");
		}
		if (attribute.getValue().length() > 255) {
			errors.rejectValue("value", "too.long");
		}
		if (attribute.getIri() != null && attribute.getIri().length() > 255) {
			errors.rejectValue("iri", "too.long");
		}
		if (attribute.getUnit() != null && attribute.getUnit().length() > 255) {
			errors.rejectValue("unit", "too.long");
		}
	}

}
