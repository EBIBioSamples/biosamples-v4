package uk.ac.ebi.biosamples.models;

import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Property;

import java.util.Set;

import org.neo4j.ogm.annotation.GraphId;
import org.neo4j.ogm.annotation.Relationship;

@NodeEntity(label = "Sample")
public class NeoSample {

	@GraphId
	private Long id;

	@Property
	private String accession;
	
	@Relationship(type = "RELATED_TO")
	private Set<NeoRelationship> relationships;

	@SuppressWarnings("unused")
	private NeoSample() {
	}

	public NeoSample(String accession) {
		this.accession = accession;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getAccession() {
		return accession;
	}

	public void setAccession(String accession) {
		this.accession = accession;
	}

	public Set<NeoRelationship> getRelationships() {
		return relationships;
	}

	public void setRelationships(Set<NeoRelationship> relationships) {
		this.relationships = relationships;
	}
	

}
