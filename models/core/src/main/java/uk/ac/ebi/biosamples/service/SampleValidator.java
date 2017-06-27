package uk.ac.ebi.biosamples.service;

import java.util.ArrayList;
import java.util.Collection;

import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.Sample;

@Service
public class SampleValidator {

	private final AttributeValidator attributeValidator;
	
	public SampleValidator(AttributeValidator attributeValidator) {
		this.attributeValidator = attributeValidator;
	}

	public Collection<String> validate(Sample sample) {
		Collection<String> errors = new ArrayList<String>();
		validate(sample,errors);
		return errors;
	}
	
	public void validate(Sample sample, Collection<String> errors) {
		
		if (sample.getRelease() == null) {
			errors.add("Must provide release date in format YYYY-MM-DDTHH:MM:SS");
		}
		if (sample.getUpdate() == null) {
			errors.add("Must provide update date in format YYYY-MM-DDTHH:MM:SS");
		}
		if (sample.getName() == null) {
			errors.add("Must provide name");
		}
		
		//TODO more validation
		for (Attribute attribute : sample.getAttributes()) {
			attributeValidator.validate(attribute, errors);
		}
	}

}
