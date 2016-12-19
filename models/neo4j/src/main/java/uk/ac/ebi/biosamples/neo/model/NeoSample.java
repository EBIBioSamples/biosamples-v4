package uk.ac.ebi.biosamples.neo.model;

import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Property;

import java.util.HashSet;
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
	
	@Relationship(type = "RELATED_TO", direction = Relationship.INCOMING)
	private Set<NeoRelationship> relationshipsIncoming;

	@SuppressWarnings("unused")
	private NeoSample() {
	}

	public NeoSample(String accession) {
		this.accession = accession;
	}

	public Long getId() {
		return id;
	}

	public String getAccession() {
		return accession;
	}

	public Set<NeoRelationship> getRelationships() {
		return relationships;
	}


	public Set<NeoRelationship> getRelationshipsIncoming() {
		return relationshipsIncoming;
	}

}
