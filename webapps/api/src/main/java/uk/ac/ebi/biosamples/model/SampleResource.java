package uk.ac.ebi.biosamples.model;

import java.time.LocalDateTime;
import java.util.SortedSet;

import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.hateoas.ResourceSupport;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.Relationship;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.service.CustomLocalDateTimeDeserializer;
import uk.ac.ebi.biosamples.service.CustomLocalDateTimeSerializer;

public class SampleResource extends ResourceSupport {
	
	@JsonIgnore
	private Sample sample;
	
	public SampleResource(Sample sample){
		this.sample = sample;
	}

	public String getAccession() {
		return sample.getAccession();
	}

	public String getName() {
		return sample.getName();
	}

	@JsonSerialize(using = CustomLocalDateTimeSerializer.class)
	@JsonDeserialize(using = CustomLocalDateTimeDeserializer.class)
	public LocalDateTime getRelease() {
		return sample.getRelease();
	}

	@JsonSerialize(using = CustomLocalDateTimeSerializer.class)
	@JsonDeserialize(using = CustomLocalDateTimeDeserializer.class)
	@LastModifiedDate
	public LocalDateTime getUpdate() {
		return sample.getUpdate();
	}

	public SortedSet<Attribute> getAttributes() {
		return sample.getAttributes();
	}

	public SortedSet<Relationship> getRelationships() {
		return sample.getRelationships();
	}

}
