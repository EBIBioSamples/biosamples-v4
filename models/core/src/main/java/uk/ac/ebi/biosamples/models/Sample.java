package uk.ac.ebi.biosamples.models;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

public class Sample {
	
	protected String accession;
	protected String name; 
	@JsonSerialize(using = CustomLocalDateTimeSerializer.class)
	@JsonDeserialize(using = CustomLocalDateTimeDeserializer.class)
	protected LocalDateTime release; 
	@JsonSerialize(using = CustomLocalDateTimeSerializer.class)
	@JsonDeserialize(using = CustomLocalDateTimeDeserializer.class)
	protected LocalDateTime update;

	protected SortedSet<Attribute> attributes;
	protected SortedSet<Relationship> relationships;

	private Sample() {
		
	}

	public String getAccession() {
		return accession;
	}

	public String getName() {
		return name;
	}

	public LocalDateTime getRelease() {
		return release;
	}

	public LocalDateTime getUpdate() {
		return update;
	}

	public SortedSet<Attribute> getAttributes() {
		return attributes;
	}

	public SortedSet<Relationship> getRelationships() {
		return relationships;
	}

	@Override
    public boolean equals(Object o) {

        if (o == this) return true;
        if (!(o instanceof Sample)) {
            return false;
        }
        Sample other = (Sample) o;
        return Objects.equals(this.name, other.name) 
        		&& Objects.equals(this.accession, other.accession)
        		&& Objects.equals(this.release, other.release)
        		&& Objects.equals(this.update, other.update)
        		&& Objects.equals(this.attributes, other.attributes)
        		&& Objects.equals(this.relationships, other.relationships);
    }
    
    @Override
    public int hashCode() {
    	return Objects.hash(name, accession, release, update, attributes, relationships);
    }
	
	static public Sample build(String name, String accession, LocalDateTime release, LocalDateTime update, Set<Attribute> attributes, Set<Relationship> relationships){
		Sample sample = new Sample();
		sample.accession = accession;
		sample.name = name;
		sample.release = release;
		sample.update = update;
		sample.attributes = new TreeSet<>();
		sample.attributes.addAll(attributes);
		sample.relationships = new TreeSet<>();
		sample.relationships.addAll(relationships);
		return sample;
	}

}
