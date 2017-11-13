package uk.ac.ebi.biosamples.mongo.model;

import java.util.Collection;
import java.util.Collections;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Document
public class MongoSampleTab {

	@Id
	@JsonIgnore
	private String id;	
	private String domain;	
	private String sampleTab;
	private Collection<String> accessions;

	private MongoSampleTab(String id, String domain, String sampleTab, Collection<String> accessions) {
		this.id = id;
		this.sampleTab = sampleTab;
		this.accessions = accessions;
	}
	
	public String getId() {
		return id;
	}
	
	public String getDomain() {
		return domain;
	}
	
	public String getSampleTab() {
		return sampleTab;
	}
	
	public Collection<String> getAccessions() {
		return Collections.unmodifiableCollection(accessions);
	}

    @JsonCreator
	public static MongoSampleTab build(String id, String domain, String sampleTab, Collection<String> accessions) {
		return new MongoSampleTab(id, domain, sampleTab, Collections.unmodifiableCollection(accessions));
	}
}
