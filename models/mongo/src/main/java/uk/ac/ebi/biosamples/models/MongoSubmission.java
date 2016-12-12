package uk.ac.ebi.biosamples.models;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

public class MongoSubmission {

	@Id
	@JsonIgnore
	public String id;

	@JsonSerialize(using = CustomLocalDateTimeSerializer.class)
	@JsonDeserialize(using = CustomLocalDateTimeDeserializer.class)
	@LastModifiedDate
	public LocalDateTime datetime;

	public MongoSample sample;
	
	public MongoSubmission(){}
	
	public MongoSubmission(MongoSample sample){
		this.sample = sample;
		this.datetime = LocalDateTime.now();
	}

	public MongoSubmission(MongoSample sample, LocalDateTime datetime){
		this.sample = sample;
		this.datetime = datetime;
	}
	
}
