package uk.ac.ebi.biosamples.neo.model;

import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Property;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import org.neo4j.ogm.annotation.GraphId;
import org.neo4j.ogm.annotation.Index;
import org.neo4j.ogm.annotation.Relationship;
import org.neo4j.ogm.annotation.typeconversion.Convert;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.neo.service.LocalDateTimeConverter;

@NodeEntity(label = "Sample")
public class NeoSample {

	@GraphId
	private Long id;

	@Property
	@Index(unique=true, primary=true)
	private String accession;

	@Property
	private String name;

	@Convert(LocalDateTimeConverter.class)
	@Property(name="update")
	private LocalDateTime update;
	@Convert(LocalDateTimeConverter.class)
	@Property(name="release")
	private LocalDateTime release;
	
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

	public String getName() {
		return name;
	}
	public LocalDateTime getUpdate() {
		return update;
	}
	public LocalDateTime getRelease() {
		return release;
	}
	
	public Set<NeoRelationship> getRelationships() {
		return relationships;
	}

	public Set<NeoAttribute> getAttributes() {
		return attributes;
	}

	public Set<NeoExternalReference> getExternalReferences() {
		return externalReferences;
	}

	@Override
    public boolean equals(Object o) {

        if (o == this) return true;
        if (!(o instanceof NeoSample)) {
            return false;
        }
        NeoSample other = (NeoSample) o;
        return Objects.equals(this.name, other.name) 
        		&& Objects.equals(this.accession, other.accession)
        		&& Objects.equals(this.release, other.release)
        		&& Objects.equals(this.update, other.update)
        		&& Objects.equals(this.attributes, other.attributes)
        		&& Objects.equals(this.relationships, other.relationships)
        		&& Objects.equals(this.externalReferences, other.externalReferences);
    }
    
    @Override
    public int hashCode() {
    	return Objects.hash(name, accession, release, update, attributes, relationships, externalReferences);
    }

    @Override
    public String toString() {
    	StringBuilder sb = new StringBuilder();
    	sb.append("NeoSample(");
    	sb.append(name);
    	sb.append(",");
    	sb.append(accession);
    	sb.append(",");
    	sb.append(release);
    	sb.append(",");
    	sb.append(update);
    	sb.append(",");
    	sb.append(attributes);
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
	public static NeoSample build(String name, String accession, LocalDateTime release, LocalDateTime update, 
			Set<NeoAttribute> attributes, Set<NeoRelationship> relationships, Set<NeoExternalReference> externalReferences) {
		NeoSample neoSample = new NeoSample();
		neoSample.accession = accession;
		neoSample.name = name;
		neoSample.release = release;
		neoSample.update = update;

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

