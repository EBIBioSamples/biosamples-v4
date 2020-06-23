package uk.ac.ebi.biosamples.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.Relationship;
import uk.ac.ebi.biosamples.model.Sample;

@Service
public class SampleValidator {

	private final AttributeValidator attributeValidator;
	private final RelationshipValidator relationshipValidator;

	public SampleValidator(AttributeValidator attributeValidator) {
		this.attributeValidator = attributeValidator;
		this.relationshipValidator = new RelationshipValidator();
	}

	public Collection<String> validate(Sample sample) {
		Collection<String> errors = new ArrayList<String>();
		validate(sample,errors);
		return errors;
	}

	public List<String> validate(Map sampleAsMap) {
		List<String> errors = new ArrayList<>();
		if (sampleAsMap.get("release") == null) {
			errors.add("Must provide release date in format YYYY-MM-DDTHH:MM:SS");
		}

		if (sampleAsMap.get("name") == null) {
			errors.add("Must provide name");
		}

		ObjectMapper mapper = new ObjectMapper();
		try {
			Sample sample = mapper.convertValue(sampleAsMap, Sample.class);
			validate(sample, errors);
		} catch (IllegalArgumentException e) {
			errors.add(e.getMessage());
		}

		return errors;
	}

	public void validate(Sample sample, Collection<String> errors) {

		if (sample.getRelease() == null) {
			errors.add("Must provide release date in format YYYY-MM-DDTHH:MM:SS");
		}

		if (sample.getName() == null) {
			errors.add("Must provide name");
		}

		//TODO more validation
		for (Attribute attribute : sample.getAttributes()) {
			attributeValidator.validate(attribute, errors);
		}

		for (Relationship rel : sample.getRelationships()) {
			errors.addAll(relationshipValidator.validate(rel, sample.getAccession()));
		}
	}

}
