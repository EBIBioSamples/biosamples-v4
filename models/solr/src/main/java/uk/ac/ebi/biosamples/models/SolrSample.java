package uk.ac.ebi.biosamples.models;

import java.time.LocalDateTime;
import java.util.Set;
import org.apache.solr.client.solrj.beans.Field;
import org.springframework.data.annotation.Id;
import org.springframework.data.solr.core.mapping.SolrDocument;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@SolrDocument(solrCoreName = "samples")
public class SolrSample {

	@Id
	protected String accession;
    @Field 
	protected String name; 
	@JsonSerialize(using = CustomLocalDateTimeSerializer.class)
	@JsonDeserialize(using = CustomLocalDateTimeDeserializer.class)
    @Field 
	protected LocalDateTime release; 
	@JsonSerialize(using = CustomLocalDateTimeSerializer.class)
	@JsonDeserialize(using = CustomLocalDateTimeDeserializer.class)
    @Field 
	protected LocalDateTime update;
	
	public SolrSample(){}
	
	static public SolrSample build(String name, String accession, LocalDateTime release, LocalDateTime update, Set<Attribute> attributes, Set<Relationship> relationships){
		SolrSample sample = new SolrSample();
		sample.accession = accession;
		sample.name = name;
		sample.release = release;
		sample.update = update;
		return sample;
	}
}
