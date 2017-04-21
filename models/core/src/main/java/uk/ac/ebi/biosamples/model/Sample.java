package uk.ac.ebi.biosamples.model;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import uk.ac.ebi.biosamples.service.CustomLocalDateTimeDeserializer;
import uk.ac.ebi.biosamples.service.CustomLocalDateTimeSerializer;
import uk.ac.ebi.biosamples.service.CustomAttributeSerializer;


@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(value={"characteristics"}, allowGetters=true)
public class Sample {
	
	protected String accession;
	protected String name; 
	
	protected LocalDateTime release; 
	protected LocalDateTime update;

	protected SortedSet<Attribute> attributes;
	protected SortedSet<Relationship> relationships;
	protected SortedSet<ExternalReference> externalReferences;
	protected SortedSet<Curation> curations;

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
	
	@JsonProperty("release")
	@JsonSerialize(using = CustomLocalDateTimeSerializer.class)
	public LocalDateTime getRelease() {
		return release;
	}

	@JsonProperty("update")
	@JsonSerialize(using = CustomLocalDateTimeSerializer.class)
	public LocalDateTime getUpdate() {
		return update;
	}

	@JsonProperty("attributes")
	public SortedSet<Attribute> getAttributes() {
		return attributes;
	}

	@Deprecated
    @JsonSerialize(using = CustomAttributeSerializer.class)
	@JsonProperty("characteristics")
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

	@JsonIgnore
	public SortedSet<Curation> getCurations() {
		return curations;
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
    	sb.append("Sample(");
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
		    

    //Used for deserializtion (JSON -> Java)
    @JsonCreator
	public static Sample build(@JsonProperty("name") String name, 
			@JsonProperty("accession") String accession, 
			@JsonProperty("release") @JsonDeserialize(using = CustomLocalDateTimeDeserializer.class) LocalDateTime release, 
			@JsonProperty("update") @JsonDeserialize(using = CustomLocalDateTimeDeserializer.class) LocalDateTime update, 
			@JsonProperty("attributes") Set<Attribute> attributes, 
			@JsonProperty("relationships") Set<Relationship> relationships, 
			@JsonProperty("externalReferences") Set<ExternalReference> externalReferences) {
    	
		Sample sample = new Sample();
		sample.accession = accession;
		sample.name = name;

		//this ensures that all components are present, even if they default to zero
		if (release != null) {
			int year = release.getYear();
			int month = release.getMonthValue();
			int dayOfMonth = release.getDayOfMonth();
			int hour = release.getHour();
			int minute = release.getMinute();
			int second = release.getSecond();
			int nano = release.getNano();			
			sample.release = LocalDateTime.of(year,month,dayOfMonth,hour,minute,second,nano);
		}
		if (update != null) {
			int year = update.getYear();
			int month = update.getMonthValue();
			int dayOfMonth = update.getDayOfMonth();
			int hour = update.getHour();
			int minute = update.getMinute();
			int second = update.getSecond();
			int nano = update.getNano();			
			sample.update = LocalDateTime.of(year,month,dayOfMonth,hour,minute,second,nano);
		}		

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
