package uk.ac.ebi.biosamples.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import uk.ac.ebi.biosamples.service.CharacteristicDeserializer;
import uk.ac.ebi.biosamples.service.CharacteristicSerializer;
import uk.ac.ebi.biosamples.service.CustomInstantDeserializer;
import uk.ac.ebi.biosamples.service.CustomInstantSerializer;

import java.time.Instant;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;


@JsonInclude(JsonInclude.Include.NON_NULL)
public class Sample implements Comparable<Sample> {
	
	protected String accession;
	protected String name; 
	
	/**
	 * This is the unique permanent ID of the AAP domain/team
	 * that owns this sample.
	 */
	protected String domain;
	
	protected Instant release; 
	protected Instant update;

	protected SortedSet<Attribute> attributes;
	protected SortedSet<Relationship> relationships;
	protected SortedSet<ExternalReference> externalReferences;

	protected Sample() {
		
	}

	@JsonProperty("accession")
	public String getAccession() {
		return accession;
	}
	@JsonIgnore
	public boolean hasAccession() {
		if ( accession != null && accession.trim().length() != 0) {
			return true;
		} else {
			return false;
		}
	}

	@JsonProperty("name")
	public String getName() {
		return name;
	}

	@JsonProperty("domain")
	public String getDomain() {
		return domain;
	}

	//DO NOT specify the JSON property value manually, must be autoinferred or errors
	@JsonSerialize(using = CustomInstantSerializer.class)
	public Instant getRelease() {
		return release;
	}

	//DO NOT specify the JSON property value manually, must be autoinferred or errors
	@JsonSerialize(using = CustomInstantSerializer.class)
	public Instant getUpdate() {
		return update;
	}

    @JsonIgnore
	public SortedSet<Attribute> getAttributes() {
		return attributes;
	}

	//DO NOT specify the JSON property value manually, must be autoinferred or errors
    @JsonSerialize(using = CharacteristicSerializer.class)
	public SortedSet<Attribute> getCharacteristics() {
		return attributes;
	}

	@JsonProperty("relationships")
	public SortedSet<Relationship> getRelationships() {
		return relationships;
	}

	@JsonProperty("externalReferences")
	public SortedSet<ExternalReference> getExternalReferences() {
		return externalReferences;
	}

	@Override
    public boolean equals(Object o) {

        if (o == this) return true;
        if (!(o instanceof Sample)) {
            return false;
        }
        Sample other = (Sample) o;
        
        //dont use update date for comparisons, too volatile
        
        return Objects.equals(this.name, other.name) 
        		&& Objects.equals(this.accession, other.accession)
        		//&& Objects.equals(this.domain, other.domain)
        		&& Objects.equals(this.release, other.release)
        		&& Objects.equals(this.attributes, other.attributes)
        		&& Objects.equals(this.relationships, other.relationships)
        		&& Objects.equals(this.externalReferences, other.externalReferences);
    }

	@Override
	public int compareTo(Sample other) {
		if (other == null) {
			return 1;
		}
		
		if (!this.accession.equals(other.accession)) {
			return this.accession.compareTo(other.accession);
		}
		
		if (!this.name.equals(other.name)) {
			return this.name.compareTo(other.name);
		}

		if (!this.release.equals(other.release)) {
			return this.release.compareTo(other.release);
		}
		
		if (!this.attributes.equals(other.attributes)) {
			if (this.attributes.size() < other.attributes.size()) {
				return -1;
			} else if (this.attributes.size() > other.attributes.size()) {
				return 1;
			} else {
				Iterator<Attribute> thisIt = this.attributes.iterator();
				Iterator<Attribute> otherIt = other.attributes.iterator();
				while (thisIt.hasNext() && otherIt.hasNext()) {
					int val = thisIt.next().compareTo(otherIt.next());
					if (val != 0) return val;
				}
			}
		}
		if (!this.relationships.equals(other.relationships)) {
			if (this.relationships.size() < other.relationships.size()) {
				return -1;
			} else if (this.relationships.size() > other.relationships.size()) {
				return 1;
			} else {
				Iterator<Relationship> thisIt = this.relationships.iterator();
				Iterator<Relationship> otherIt = other.relationships.iterator();
				while (thisIt.hasNext() && otherIt.hasNext()) {
					int val = thisIt.next().compareTo(otherIt.next());
					if (val != 0) return val;
				}
			}
		}
		if (!this.externalReferences.equals(other.externalReferences)) {
			if (this.externalReferences.size() < other.externalReferences.size()) {
				return -1;
			} else if (this.externalReferences.size() > other.externalReferences.size()) {
				return 1;
			} else {
				Iterator<ExternalReference> thisIt = this.externalReferences.iterator();
				Iterator<ExternalReference> otherIt = other.externalReferences.iterator();
				while (thisIt.hasNext() && otherIt.hasNext()) {
					int val = thisIt.next().compareTo(otherIt.next());
					if (val != 0) return val;
				}
			}
		}
		return 0;
	}
    
    @Override
    public int hashCode() {
    	//dont put update date in the hash because its not in comparison
    	return Objects.hash(name, accession, release, attributes, relationships, externalReferences);
    }
    
    @Override
    public String toString() {
    	StringBuilder sb = new StringBuilder();
    	sb.append("Sample(");
    	sb.append(name);
    	sb.append(",");
    	sb.append(accession);
    	sb.append(",");
    	sb.append(domain);
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

    //Used for deserializtion (JSON -> Java)
    @JsonCreator
	public static Sample build(@JsonProperty("name") String name, 
			@JsonProperty("accession") String accession,  
			@JsonProperty("domain") String domain,
			@JsonProperty("release") @JsonDeserialize(using = CustomInstantDeserializer.class) Instant release, 
			@JsonProperty("update") @JsonDeserialize(using = CustomInstantDeserializer.class) Instant update,
			@JsonProperty("characteristics") @JsonDeserialize(using = CharacteristicDeserializer.class) Set<Attribute> attributes,
			@JsonProperty("relationships") Set<Relationship> relationships, 
			@JsonProperty("externalReferences") Set<ExternalReference> externalReferences) {
    	
		Sample sample = new Sample();
		
		if (accession != null) {
			sample.accession = accession.trim();
		}
		
		if (name == null ) throw new IllegalArgumentException("Sample name must be provided");
		sample.name = name.trim();
		
		if (domain != null) {
			sample.domain = domain.trim();
		}
		
		if (update == null ) throw new IllegalArgumentException("Sample update must be provided");
		sample.update = update;
		
		if (release == null ) throw new IllegalArgumentException("Sample release must be provided");
		sample.release = release;

		sample.attributes = new TreeSet<>();
		if (attributes != null) {
			sample.attributes.addAll(attributes);
		}

		sample.relationships = new TreeSet<>();
		if (relationships != null) {
			sample.relationships.addAll(relationships);
		}

		sample.externalReferences = new TreeSet<>();
		if (externalReferences != null) {
			sample.externalReferences.addAll(externalReferences);
		}	
		
		return sample;
	}


}
