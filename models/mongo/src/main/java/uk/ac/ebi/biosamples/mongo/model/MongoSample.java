package uk.ac.ebi.biosamples.mongo.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.Contact;
import uk.ac.ebi.biosamples.model.Organization;
import uk.ac.ebi.biosamples.model.Publication;
import uk.ac.ebi.biosamples.model.structured.AbstractData;
import uk.ac.ebi.biosamples.service.CustomInstantDeserializer;
import uk.ac.ebi.biosamples.service.CustomInstantSerializer;

import java.time.Instant;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Document
public class MongoSample {
	
	@Id
	protected String accession;
	@Indexed(background=true)
	protected String accessionPrefix;
	@Indexed(background=true)
	protected Integer accessionNumber;

	protected String name; 
	
	/**
	 * This is the unique permanent ID of the AAP domain/team
	 * that owns this sample.
	 */
	@Indexed(background=true)
	protected String domain;
	
	@JsonSerialize(using = CustomInstantSerializer.class)
	@JsonDeserialize(using = CustomInstantDeserializer.class)
	@Indexed(background=true)
	protected Instant release; 
	@JsonSerialize(using = CustomInstantSerializer.class)
	@JsonDeserialize(using = CustomInstantDeserializer.class)
	@LastModifiedDate
	@Indexed(background=true)
	protected Instant update;

	protected SortedSet<Attribute> attributes;
	protected SortedSet<MongoRelationship> relationships;
	protected SortedSet<MongoExternalReference> externalReferences;

	protected SortedSet<Organization> organizations;
	protected SortedSet<Contact> contacts;
	protected SortedSet<Publication> publications;

	//	@JsonSerialize(using = AbstractDataSerializer.class)
	protected Set<AbstractData> data;

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
	
	public String getAccessionPrefix() {
		return accessionPrefix;
	}
	
	public Integer getAccessionNumber() {
		return accessionNumber;
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

	public SortedSet<Contact> getContacts() {
		return contacts;
	}

	public SortedSet<Publication> getPublications() {
		return publications;
	}

	public Set<AbstractData> getData() {
		return data;
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
        		&& Objects.equals(this.organizations,  other.organizations)
        		&& Objects.equals(this.contacts,  other.contacts)
        		&& Objects.equals(this.publications,  other.publications);
    }
    
    @Override
    public int hashCode() {
    	return Objects.hash(name, accession, domain, release, update, attributes, relationships, externalReferences, organizations, contacts, publications);
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
    	sb.append(",");
    	sb.append(contacts);
    	sb.append(",");
    	sb.append(publications);
    	sb.append(",");
    	sb.append(data);
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
    		@JsonProperty("structured") Set<AbstractData> structuredData,
    		@JsonProperty("relationships") Set<MongoRelationship> relationships, 
    		@JsonProperty("externalReferences") SortedSet<MongoExternalReference> externalReferences, 
    		@JsonProperty("organizations") SortedSet<Organization> organizations, 
    		@JsonProperty("contacts") SortedSet<Contact> contacts,
    		@JsonProperty("publications") SortedSet<Publication> publications) {
		
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

		sample.data = new HashSet<>();
		if (structuredData != null && structuredData.size() > 0) {
			sample.data.addAll(structuredData);
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

		sample.contacts = new TreeSet<>();
		if (contacts != null && contacts.size() > 0) {
			sample.contacts.addAll(contacts);
		}

		sample.publications = new TreeSet<>();
		if (publications != null && publications.size() > 0) {
			sample.publications.addAll(publications);
		}
		
		//split accession into prefix & number, if possible
        Pattern r = Pattern.compile("^(\\D+)(\\d+)$");
        if (accession != null) { 
	        Matcher m = r.matcher(accession);
	        if (m.matches() && m.groupCount() == 2) {
		        sample.accessionPrefix = m.group(1);
		        sample.accessionNumber = Integer.parseInt(m.group(2));
	        }
        }
		
		return sample;
	}

}
