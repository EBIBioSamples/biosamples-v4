package uk.ac.ebi.biosamples.mongo.model;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import uk.ac.ebi.biosamples.models.Attribute;
import uk.ac.ebi.biosamples.models.CustomLocalDateTimeDeserializer;
import uk.ac.ebi.biosamples.models.CustomLocalDateTimeSerializer;
import uk.ac.ebi.biosamples.models.Relationship;

public class MongoSample {
	
	@Id
	public String accession;

	protected String name; 
	
	@JsonSerialize(using = CustomLocalDateTimeSerializer.class)
	@JsonDeserialize(using = CustomLocalDateTimeDeserializer.class)
	protected LocalDateTime release; 
	@JsonSerialize(using = CustomLocalDateTimeSerializer.class)
	@JsonDeserialize(using = CustomLocalDateTimeDeserializer.class)
	@LastModifiedDate
	protected LocalDateTime update;

	protected SortedSet<Attribute> attributes;
	protected SortedSet<Relationship> relationships;

	private MongoSample() {
		
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
	
	public void addAttribute(Attribute attribute) {
		if (attributes == null) {
			attributes = new TreeSet<>();
		}
		attributes.add(attribute);
	}

	public SortedSet<Relationship> getRelationships() {
		return relationships;
	}
	
	public void addRelationship(Relationship relationship) {
		if (relationships == null) {
			relationships = new TreeSet<>();
		}
		relationships.add(relationship);
	}

	@Override
    public boolean equals(Object o) {

        if (o == this) return true;
        if (!(o instanceof MongoSample)) {
            return false;
        }
        MongoSample other = (MongoSample) o;
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
	
	static public MongoSample build(String name, String accession, LocalDateTime release, LocalDateTime update, Set<Attribute> attributes, Set<Relationship> relationships){
		MongoSample sample = new MongoSample();
		sample.accession = accession;
		sample.name = name;
		sample.release = release;
		sample.update = update;
		
		if (attributes != null && attributes.size() > 0) {
			for (Attribute attribute : attributes) {
				sample.addAttribute(attribute);
			}
		}

		if (relationships != null && relationships.size() > 0) {
			for (Relationship relationship : relationships) {
				sample.addRelationship(relationship);
			}
		}
		return sample;
	}

}
