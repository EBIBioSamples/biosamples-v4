package uk.ac.ebi.biosamples.models;

import java.util.SortedSet;
import java.util.TreeSet;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class MongoSample {

	@Id
	public String id;
	
	@Indexed(unique=true)
	public String accession;

	public SortedSet<Attribute> attributes = new TreeSet<>();

	public MongoSample() {
	}

}
