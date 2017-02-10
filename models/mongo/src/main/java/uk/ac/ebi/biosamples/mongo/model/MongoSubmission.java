package uk.ac.ebi.biosamples.mongo.model;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import uk.ac.ebi.biosamples.model.CustomLocalDateTimeDeserializer;
import uk.ac.ebi.biosamples.model.CustomLocalDateTimeSerializer;
import uk.ac.ebi.biosamples.model.Sample;

public class MongoSubmission {

	@Id
	@JsonIgnore
	public String id;

	@JsonSerialize(using = CustomLocalDateTimeSerializer.class)
	@JsonDeserialize(using = CustomLocalDateTimeDeserializer.class)
	@LastModifiedDate
	public LocalDateTime datetime;

	public Sample sample;
	
	public MongoSubmission(){}
	
	public MongoSubmission(Sample sample){
		this.sample = sample;
		this.datetime = LocalDateTime.now();
	}

	public MongoSubmission(Sample sample, LocalDateTime datetime){
		this.sample = sample;
		this.datetime = datetime;
	}
	
}
