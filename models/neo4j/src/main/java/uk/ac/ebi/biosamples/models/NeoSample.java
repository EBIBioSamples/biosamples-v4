package uk.ac.ebi.biosamples.models;

import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Property;

import java.util.HashSet;
import java.util.Set;

import org.neo4j.ogm.annotation.GraphId;
import org.neo4j.ogm.annotation.Relationship;

@NodeEntity(label="Sample")
public class NeoSample {

	@GraphId
	private Long id;

	@Property
	private String accession;

	@Relationship(type = "SAME_AS", direction = Relationship.UNDIRECTED)
	private Set<NeoSample> sameAs;

	@Relationship(type = "DERIVED_TO", direction = Relationship.INCOMING)
	private Set<NeoSample> derivedTo;

	@Relationship(type = "CHILD_OF", direction = Relationship.OUTGOING)
	private Set<NeoSample> childOf;

	@Relationship(type = "DERIVED_TO", direction = Relationship.OUTGOING)
	private Set<NeoSample> derivedFrom;
	
	//TODO make this handle any generic relationship type

	private NeoSample() {
	}
	
	public NeoSample(String accession) {
		this.accession = accession;
		this.sameAs = new HashSet<>();
		this.derivedTo = new HashSet<>();
		this.childOf = new HashSet<>();
		this.derivedFrom = new HashSet<>();
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

	public Set<NeoSample> getSameAs() {
		return sameAs;
	}

	public void setSameAs(Set<NeoSample> sameAs) {
		this.sameAs = sameAs;
	}

	public Set<NeoSample> getDerivedTo() {
		return derivedTo;
	}

	public void setDerivedTo(Set<NeoSample> derivedTo) {
		this.derivedTo = derivedTo;
	}

	public Set<NeoSample> getChildren() {
		return childOf;
	}

	public void setChildren(Set<NeoSample> children) {
		this.childOf = children;
	}

	public Set<NeoSample> getDerivedFrom() {
		return derivedFrom;
	}

	public void setDerivedFrom(Set<NeoSample> derivedFrom) {
		this.derivedFrom = derivedFrom;
	};

}
