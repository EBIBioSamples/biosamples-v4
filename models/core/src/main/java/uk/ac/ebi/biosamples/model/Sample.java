package uk.ac.ebi.biosamples.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import uk.ac.ebi.biosamples.service.CustomLocalDateTimeDeserializer;
import uk.ac.ebi.biosamples.service.CustomLocalDateTimeSerializer;
import uk.ac.ebi.biosamples.service.CustomSampleDeserializer;
import uk.ac.ebi.biosamples.service.CustomSampleSerializer;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;


@JsonSerialize(using=CustomSampleSerializer.class)
@JsonDeserialize(using=CustomSampleDeserializer.class)
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
	protected SortedSet<URI> externalReferences;

	protected Sample() {
		
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

	public SortedSet<URI> getExternalReferences() {
		return externalReferences;
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
		
	static public Sample build(String name, String accession, LocalDateTime release, LocalDateTime update, 
			Set<Attribute> attributes, Set<Relationship> relationships, SortedSet<URI> externalReferences){
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
		
		
		if (attributes == null || attributes.size() == 0) {
			sample.attributes = null;
		} else {
			sample.attributes = new TreeSet<>();
			sample.attributes.addAll(attributes);
		}

		if (relationships == null || relationships.size() == 0) {
			sample.relationships = null;
		} else {
			sample.relationships = new TreeSet<>();
			sample.relationships.addAll(relationships);
		}

		if (externalReferences == null || externalReferences.size() == 0) {
			sample.externalReferences = null;
		} else {
			sample.externalReferences = new TreeSet<>();
			sample.externalReferences.addAll(externalReferences);
		}	
		
		return sample;
	}

}
