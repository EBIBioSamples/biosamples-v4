package uk.ac.ebi.biosamples.model;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.hash.Hashing;

public class ExternalReferenceLink implements Comparable<ExternalReferenceLink> {

	private final String sample;
	private final ExternalReference externalReference;
	private final String hash;
	
	private ExternalReferenceLink(String sample, ExternalReference externalReference, String hash) {
		this.sample = sample;
		this.externalReference = externalReference;
		this.hash = hash;
	}
	
	
	public String getSample() {
		return sample;
	}
	
	@JsonIgnore
	public ExternalReference getExternalReference(){
		return externalReference;
	}
	
	public String getUrl() {
		return externalReference.getUrl();
	}

	@JsonIgnore
	public String getId() {
		return hash;
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

    //Used for deserializtion (JSON -> Java)
    @JsonCreator
	static public ExternalReferenceLink build(@JsonProperty("sample") String sample, 
			@JsonProperty("url") String url) {
    	ExternalReference externalReference = ExternalReference.build(url);
    	
    	String hash = Hashing.sha256().newHasher()
			.putUnencodedChars(externalReference.getUrl())
			.putUnencodedChars(sample)
			.hash().toString();
    	
		return new ExternalReferenceLink(sample, externalReference, hash);
	}
}
