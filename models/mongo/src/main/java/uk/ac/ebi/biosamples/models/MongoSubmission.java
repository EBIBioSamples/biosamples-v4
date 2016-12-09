package uk.ac.ebi.biosamples.models;

import java.time.LocalDateTime;
import org.springframework.data.annotation.Id;
import com.fasterxml.jackson.annotation.JsonIgnore;

public class MongoSubmission {

	@Id
	@JsonIgnore
	public String id;

	public MongoSample sample;
	
	public LocalDateTime datetime;
	
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
