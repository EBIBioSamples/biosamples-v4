package uk.ac.ebi.biosamples.service;

import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.Sample;

@Service
public class SampleValidator implements Validator {

	private final AttributeValidator attributeValidator;
	
	public SampleValidator(AttributeValidator attributeValidator) {
		this.attributeValidator = attributeValidator;
	}
	
	@Override
	public boolean supports(Class<?> clazz) {
		return Sample.class.isAssignableFrom(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		Sample sample = (Sample) target;
		
        errors.pushNestedPath("attributes");
		for (Attribute attribute : sample.getAttributes()) {
            ValidationUtils.invokeValidator(this.attributeValidator, attribute, errors);
		}
        errors.popNestedPath();
        //TODO more validation
	}

}
