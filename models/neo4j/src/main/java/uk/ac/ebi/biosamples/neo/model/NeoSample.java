package uk.ac.ebi.biosamples.neo.model;

import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Property;

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

	@Property
	private String name;
	
	@Relationship(type = "RELATED_TO", direction = Relationship.UNDIRECTED)
	private Set<NeoRelationship> relationships;

    @Relationship(type = "HAS_EXTERNAL_REFERENCE")
	private Set<NeoExternalReference> externalReferences;

    @Relationship(type = "HAS_ATTRIBUTE")
	private Set<NeoAttribute> attributes;

	@SuppressWarnings("unused")
	private NeoSample() {
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

	public Set<NeoAttribute> getAttributes() {
		return attributes;
	}

	public void addRelationships(NeoRelationship relationship) {
		if (relationships == null) {
			relationships = new HashSet<>();
		}
		relationships.add(relationship);
	}

	public Set<NeoExternalReference> getExternalReferences() {
		return externalReferences;
	}

	public void addExternalReference(NeoExternalReference reference) {
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

    
    /**
     * Create a new sample node with its attributes and external references and relationships
     * 
     * @param accession
     * @param name
     * @param attributes
     * @param relationships
     * @param externalReferences
     * @return
     */
	public static NeoSample create(String accession, String name, Set<NeoAttribute> attributes,
			Set<NeoRelationship> relationships,  Set<NeoExternalReference> externalReferences) {
		NeoSample neoSample = new NeoSample();
		neoSample.accession = accession;
		neoSample.name = name;

		if (attributes == null || attributes.size() == 0) {
			neoSample.attributes = null;
		} else {
			neoSample.attributes = new HashSet<>();
			neoSample.attributes.addAll(attributes);
		}
		
		if (relationships == null || relationships.size() == 0) {
			neoSample.relationships = null;
		} else {
			neoSample.relationships = new HashSet<>();
			neoSample.relationships.addAll(relationships);
		}
		
		if (externalReferences == null || externalReferences.size() == 0) {
			neoSample.externalReferences = null;
		} else {
			neoSample.externalReferences = new HashSet<>();
			neoSample.externalReferences.addAll(externalReferences);
		}	
		
		return neoSample;
	}

	/**
	 * Create a sample node object with only an accession. This is useful for using as referencces
	 * in building relationships
	 * 
	 * @param accession
	 * @return
	 */
    public static NeoSample create(String accession) {
		NeoSample neoSample = new NeoSample();
		neoSample.accession = accession;
		return neoSample;    	
    }
	
}

