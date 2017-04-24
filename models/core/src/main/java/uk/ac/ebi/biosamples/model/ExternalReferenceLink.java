package uk.ac.ebi.biosamples.model;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ExternalReferenceLink implements Comparable<ExternalReferenceLink> {

	private final String sample;
	private final String externalReference;
	private final Long id;
	
	private ExternalReferenceLink(String sample, String externalReference, Long id) {
		this.sample = sample;
		this.externalReference = externalReference;
		this.id = id;
	}

	public Long getId() {
		return id;
	}
	
	@Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof ExternalReferenceLink)) {
            return false;
        }
        ExternalReferenceLink other = (ExternalReferenceLink) o;
        return Objects.equals(this.externalReference, other.externalReference)
        		&& Objects.equals(this.sample, other.sample);
    }
    
    @Override
    public int hashCode() {
    	return Objects.hash(sample, externalReference);
    }

	@Override
	public int compareTo(ExternalReferenceLink other) {
		if (other == null) {
			return 1;
		}
		
		if (!this.sample.equals(other.sample)) {
			return this.sample.compareTo(other.sample);
		}
		if (!this.externalReference.equals(other.externalReference)) {
			return this.externalReference.compareTo(other.externalReference);
		}
		return 0;
	}	

    @Override
    public String toString() {
    	StringBuilder sb = new StringBuilder();
    	sb.append("ExternalReference(");
    	sb.append(this.sample);
    	sb.append(",");
    	sb.append(this.externalReference);
    	sb.append(")");
    	return sb.toString();
    }

	
	public String getSample() {
		return sample;
	}

	public String getExternalReference() {
		return externalReference;
	}

    //Used for deserializtion (JSON -> Java)
    @JsonCreator
	static public ExternalReferenceLink build(@JsonProperty("sample") String sample, 
			@JsonProperty("url") String externalReference) {
		return ExternalReferenceLink.build(sample, externalReference, null);
	}

	static public ExternalReferenceLink build(String sample, 
			String externalReference, 
			Long id) {
		return new ExternalReferenceLink(sample, externalReference, id);
	}
}
