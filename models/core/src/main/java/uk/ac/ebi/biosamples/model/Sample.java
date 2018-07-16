package uk.ac.ebi.biosamples.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import uk.ac.ebi.biosamples.model.structured.AbstractData;
import uk.ac.ebi.biosamples.service.CharacteristicDeserializer;
import uk.ac.ebi.biosamples.service.CharacteristicSerializer;
import uk.ac.ebi.biosamples.service.CustomInstantDeserializer;
import uk.ac.ebi.biosamples.service.CustomInstantSerializer;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.TemporalAccessor;
import java.util.*;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;
import static java.time.format.DateTimeFormatter.ISO_LOCAL_TIME;


@JsonInclude(JsonInclude.Include.NON_EMPTY)
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
	protected SortedSet<AbstractData> data;
	protected SortedSet<Relationship> relationships;
	protected SortedSet<ExternalReference> externalReferences;

	protected SortedSet<Organization> organizations;
	protected SortedSet<Contact> contacts;
	protected SortedSet<Publication> publications;


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

	@JsonProperty(value="releaseDate", access=JsonProperty.Access.READ_ONLY)
	public String getReleaseDate() {
		return ZonedDateTime.ofInstant(release, ZoneOffset.UTC).format(ISO_LOCAL_DATE);
	}

	@JsonProperty(value="updateDate", access=JsonProperty.Access.READ_ONLY)
	public String getUpdateDate() {
		return ZonedDateTime.ofInstant(update, ZoneOffset.UTC).format(ISO_LOCAL_DATE);
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

	public SortedSet<AbstractData> getData() {
		return data;
	}

	@JsonProperty("relationships")
	public SortedSet<Relationship> getRelationships() {
		return relationships;
	}

	@JsonProperty("externalReferences")
	public SortedSet<ExternalReference> getExternalReferences() {
		return externalReferences;
	}

	@JsonProperty("organization")
	public SortedSet<Organization> getOrganizations() {
		return organizations;
	}

	@JsonProperty("contact")
	public SortedSet<Contact> getContacts() {
		return contacts;
	}

	@JsonProperty("publications")
	public SortedSet<Publication> getPublications() {
		return publications;
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
        		&& Objects.equals(this.domain, other.domain)
        		&& Objects.equals(this.release, other.release)
        		&& Objects.equals(this.attributes, other.attributes)
				&& Objects.equals(this.data, other.data)
        		&& Objects.equals(this.relationships, other.relationships)
        		&& Objects.equals(this.externalReferences, other.externalReferences)
        		&& Objects.equals(this.organizations, other.organizations)
        		&& Objects.equals(this.contacts, other.contacts)
        		&& Objects.equals(this.publications, other.publications);
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
		if (!this.organizations.equals(other.organizations)) {
			if (this.organizations.size() < other.organizations.size()) {
				return -1;
			} else if (this.organizations.size() > other.organizations.size()) {
				return 1;
			} else {
				Iterator<Organization> thisIt = this.organizations.iterator();
				Iterator<Organization> otherIt = other.organizations.iterator();
				while (thisIt.hasNext() && otherIt.hasNext()) {
					int val = thisIt.next().compareTo(otherIt.next());
					if (val != 0) return val;
				}
			}
		}
		if (!this.contacts.equals(other.contacts)) {
			if (this.contacts.size() < other.contacts.size()) {
				return -1;
			} else if (this.contacts.size() > other.contacts.size()) {
				return 1;
			} else {
				Iterator<Contact> thisIt = this.contacts.iterator();
				Iterator<Contact> otherIt = other.contacts.iterator();
				while (thisIt.hasNext() && otherIt.hasNext()) {
					int val = thisIt.next().compareTo(otherIt.next());
					if (val != 0) return val;
				}
			}
		}
		if (!this.publications.equals(other.publications)) {
			if (this.publications.size() < other.publications.size()) {
				return -1;
			} else if (this.publications.size() > other.publications.size()) {
				return 1;
			} else {
				Iterator<Publication> thisIt = this.publications.iterator();
				Iterator<Publication> otherIt = other.publications.iterator();
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
    	return Objects.hash(name, accession, release, attributes, relationships, externalReferences, organizations, publications);
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
    	sb.append(",");
    	sb.append(organizations);
    	sb.append(",");
    	sb.append(contacts);
    	sb.append(",");
    	sb.append(publications);
    	sb.append(")");
    	return sb.toString();
    }
    
	public static Sample build( String name, 
			 String accession,  
			String domain,
			Instant release, 
			Instant update,
			Set<Attribute> attributes,
			Set<Relationship> relationships,
			Set<ExternalReference> externalReferences) {
    	return build(name, accession, domain, release, update, attributes, relationships, externalReferences, null, null, null);
    }

    //Used for deserializtion (JSON -> Java)
    @JsonCreator
	public static Sample build(@JsonProperty("name") String name, 
			@JsonProperty("accession") String accession,  
			@JsonProperty("domain") String domain,
			@JsonProperty("release") @JsonDeserialize(using = CustomInstantDeserializer.class) Instant release, 
			@JsonProperty("update") @JsonDeserialize(using = CustomInstantDeserializer.class) Instant update,
			@JsonProperty("characteristics") @JsonDeserialize(using = CharacteristicDeserializer.class) Collection<Attribute> attributes,
			@JsonProperty("relationships") Collection<Relationship> relationships, 
			@JsonProperty("externalReferences") Collection<ExternalReference> externalReferences,
			@JsonProperty("organization") Collection<Organization> organizations, 
			@JsonProperty("contact") Collection<Contact> contacts, 
			@JsonProperty("publications") Collection<Publication> publications ) {
    	
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

		sample.organizations = new TreeSet<>();
		if (organizations != null) {
			sample.organizations.addAll(organizations);
		}	

		sample.contacts = new TreeSet<>();
		if (contacts != null) {
			sample.contacts.addAll(contacts);
		}	

		sample.publications = new TreeSet<>();
		if (publications != null) {
			sample.publications.addAll(publications);
		}
		
		return sample;
	}

	public static class Builder {

		protected String accession;
		protected String name;
		protected String domain;

		protected Instant release = Instant.now();
		protected Instant update = Instant.now();

		protected SortedSet<Attribute> attributes = new TreeSet<>();
		protected SortedSet<Relationship> relationships = new TreeSet<>();
		protected SortedSet<ExternalReference> externalReferences = new TreeSet<>();
		protected SortedSet<Organization> organizations = new TreeSet<>();
		protected SortedSet<Contact> contacts = new TreeSet<>();
		protected SortedSet<Publication> publications = new TreeSet<>();

		public Builder(String accession, String name) {
			this.name = name;
			this.accession = accession;
		}

		public Builder withDomain(String domain) {
			this.domain = domain;
			return this;
		}

		public Builder withReleaseDate(String release) {
			this.release = parseDateTime(release).toInstant();
			return this;
		}

		public Builder withReleaseDate(Instant release) {
			this.release = release;
			return this;
		}

		public Builder withUpdateDate(Instant update) {
			this.update = update;
			return this;
		}

		public Builder withUpdateDate(String update) {
			this.update = parseDateTime(update).toInstant();
			return this;
		}

		public Builder withAttribute(Attribute attribute) {
			this.attributes.add(attribute);
			return this;
		}

		public Builder withRelationship(Relationship relationship) {
			this.relationships.add(relationship);
			return this;
		}

		public Builder withExternalReference(ExternalReference externalReference) {
			this.externalReferences.add(externalReference);
			return this;
		}

		public Builder withOrganization(Organization organization) {
			this.organizations.add(organization);
			return this;
		}

		public Builder withContact(Contact contact) {
			this.contacts.add(contact);
			return this;
		}

		public Builder withPublication(Publication publication) {
			this.publications.add(publication);
			return this;
		}

		public Sample build() {
			return Sample.build(name, accession, domain, release, update,
					attributes, relationships, externalReferences,
					organizations, contacts, publications);
		}

		private ZonedDateTime parseDateTime(String datetimeString) {
			if (datetimeString.isEmpty()) return null;
			TemporalAccessor temporalAccessor = getFormatter().parseBest(datetimeString,
					ZonedDateTime::from, LocalDateTime::from, LocalDate::from);
			if (temporalAccessor instanceof ZonedDateTime) {
				return (ZonedDateTime) temporalAccessor;
			} else if (temporalAccessor instanceof LocalDateTime) {
				return ((LocalDateTime) temporalAccessor).atZone(ZoneId.of("UTC"));
			} else {
				return ((LocalDate) temporalAccessor).atStartOfDay(ZoneId.of("UTC"));
			}

		}


		private DateTimeFormatter getFormatter() {
			return new DateTimeFormatterBuilder()
					.parseCaseInsensitive()
					.append(ISO_LOCAL_DATE)
					.optionalStart()           // time made optional
					.appendLiteral('T')
					.append(ISO_LOCAL_TIME)
					.optionalStart()           // zone and offset made optional
					.appendOffsetId()
					.optionalStart()
					.appendLiteral('[')
					.parseCaseSensitive()
					.appendZoneRegionId()
					.appendLiteral(']')
					.optionalEnd()
					.optionalEnd()
					.optionalEnd()
					.toFormatter();
		}
	}


}
