package uk.ac.ebi.biosamples.neo.model;

import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Property;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import org.neo4j.ogm.annotation.GraphId;
import org.neo4j.ogm.annotation.Index;
import org.neo4j.ogm.annotation.Relationship;

@NodeEntity(label = "Sample")
public class NeoSample {

	@GraphId
	private Long id;

	@Property
	@Index(unique=true, primary=true)
	private String accession;
	
	@Relationship(type = "RELATED_TO", direction = Relationship.UNDIRECTED)
	private Set<NeoRelationship> relationships;

    @Relationship(type = "EXTERNAL_REFERENCE")
	private Set<NeoUrl> externalReferences;

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

	public void addRelationships(NeoRelationship relationship) {
		if (relationships == null) {
			relationships = new HashSet<>();
		}
		relationships.add(relationship);
	}

	public Set<NeoUrl> getExternalReferences() {
		return externalReferences;
	}

	public void addExternalReference(NeoUrl reference) {
		if (externalReferences == null) {
			externalReferences = new HashSet<>();
		}
		externalReferences.add(reference);
	}

    @Override
    public String toString() {
    	StringBuilder sb = new StringBuilder();
    	sb.append("NeoSample(");
    	sb.append(accession);
    	sb.append(",");
    	sb.append(relationships);
    	sb.append(",");
    	sb.append(externalReferences);
    	sb.append(")");
    	return sb.toString();
    }

	public static NeoSample create(String accession, Set<NeoRelationship> relationships, Set<NeoUrl> externalReferences) {
		NeoSample neoSample = new NeoSample(accession);

		if (relationships == null || relationships.size() == 0) {
			neoSample.relationships = null;
		} else {
			neoSample.relationships = new TreeSet<>();
			neoSample.relationships.addAll(relationships);
		}

		
		if (externalReferences == null || externalReferences.size() == 0) {
			neoSample.externalReferences = null;
		} else {
			neoSample.externalReferences = new TreeSet<>();
			neoSample.externalReferences.addAll(externalReferences);
		}	
		
		return neoSample;
	}
}

