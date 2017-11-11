package uk.ac.ebi.biosamples.mongo.model;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.Organization;
import uk.ac.ebi.biosamples.service.CustomInstantDeserializer;
import uk.ac.ebi.biosamples.service.CustomInstantSerializer;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Document
public class MongoSample {
	
	@Id
	public String accession;

	protected String name; 
	
	/**
	 * This is the unique permanent ID of the AAP domain/team
	 * that owns this sample.
	 */
	protected String domain;
	
	@JsonSerialize(using = CustomInstantSerializer.class)
	@JsonDeserialize(using = CustomInstantDeserializer.class)
	protected Instant release; 
	@JsonSerialize(using = CustomInstantSerializer.class)
	@JsonDeserialize(using = CustomInstantDeserializer.class)
	@LastModifiedDate
	protected Instant update;

	protected SortedSet<Attribute> attributes;
	protected SortedSet<MongoRelationship> relationships;
	protected SortedSet<MongoExternalReference> externalReferences;

	protected SortedSet<Organization> organizations;

	@JsonIgnore
	public boolean hasAccession() {
		if ( accession != null && accession.trim().length() != 0) {
			return true;
		} else {
			return false;
		}
	}
	
	public String getAccession() {
		return accession;
	}

	public String getName() {
		return name;
	}

	public String getDomain() {
		return domain;
	}

	public Instant getRelease() {
		return release;
	}

	public Instant getUpdate() {
		return update;
	}

	public SortedSet<Attribute> getAttributes() {
		return attributes;
	}

	public SortedSet<MongoRelationship> getRelationships() {
		return relationships;
	}

	public SortedSet<MongoExternalReference> getExternalReferences() {
		return externalReferences;
	}

	public SortedSet<Organization> getOrganizations() {
		return organizations;
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
        		&& Objects.equals(this.domain, other.domain)
        		&& Objects.equals(this.release, other.release)
        		&& Objects.equals(this.update, other.update)
        		&& Objects.equals(this.attributes, other.attributes)
        		&& Objects.equals(this.relationships, other.relationships)
        		&& Objects.equals(this.externalReferences, other.externalReferences)
        		&& Objects.equals(this.organizations,  other.organizations);
    }
    
    @Override
    public int hashCode() {
    	return Objects.hash(name, accession, domain, release, update, attributes, relationships, externalReferences, organizations);
    }
    

    @Override
    public String toString() {
    	StringBuilder sb = new StringBuilder();
    	sb.append("MongoSample(");
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
    	sb.append(",");
    	sb.append(organizations);
    	sb.append(")");
    	return sb.toString();
    }
    

    @JsonCreator
    public static MongoSample build(@JsonProperty("name") String name, 
    		@JsonProperty("accession") String accession, 
			@JsonProperty("domain") String domain,
    		@JsonProperty("release") Instant release, 
    		@JsonProperty("update") Instant update, 
    		@JsonProperty("attributes") Set<Attribute> attributes, 
    		@JsonProperty("relationships") Set<MongoRelationship> relationships, 
    		@JsonProperty("externalReferences") SortedSet<MongoExternalReference> externalReferences, 
    		@JsonProperty("organizations") SortedSet<Organization> organizations) {
		
		MongoSample sample = new MongoSample();
		
		sample.accession = accession;		
		sample.name = name;
		sample.domain = domain;
		sample.release = release;
		sample.update = update;

		sample.attributes = new TreeSet<>();
		if (attributes != null && attributes.size() > 0) {
			sample.attributes.addAll(attributes);
		}

		sample.relationships = new TreeSet<>();
		if (relationships != null && relationships.size() > 0) {
			sample.relationships.addAll(relationships);
		}

		sample.externalReferences = new TreeSet<>();
		if (externalReferences != null && externalReferences.size() > 0) {
			sample.externalReferences.addAll(externalReferences);
		}

		sample.organizations = new TreeSet<>();
		if (organizations != null && organizations.size() > 0) {
			sample.organizations.addAll(organizations);
		}
		
		return sample;
	}

}
