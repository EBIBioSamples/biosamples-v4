package uk.ac.ebi.biosamples.model;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ExternalReferenceLink implements Comparable<ExternalReferenceLink> {

	private final String sample;
	private final String url;
	private final String hash;
	
	private ExternalReferenceLink(String sample, String url, String hash) {
		this.sample = sample;
		this.url = url;
		this.hash = hash;
	}
	
	
	public String getSample() {
		return sample;
	}
	
	public String getUrl() {
		return url;
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
        return Objects.equals(this.url, other.url)
        		&& Objects.equals(this.sample, other.sample);
    }
    
    @Override
    public int hashCode() {
    	return Objects.hash(sample, url);
    }

	@Override
	public int compareTo(ExternalReferenceLink other) {
		if (other == null) {
			return 1;
		}
		
		if (!this.sample.equals(other.sample)) {
			return this.sample.compareTo(other.sample);
		}
		if (!this.url.equals(other.url)) {
			return this.url.compareTo(other.url);
		}
		return 0;
	}	

    @Override
    public String toString() {
    	StringBuilder sb = new StringBuilder();
    	sb.append("ExternalReference(");
    	sb.append(this.sample);
    	sb.append(",");
    	sb.append(this.url);
    	sb.append(")");
    	return sb.toString();
    }

    //Used for deserializtion (JSON -> Java)
    @JsonCreator
	static public ExternalReferenceLink build(@JsonProperty("sample") String sample, 
			@JsonProperty("url") String url) {
		return ExternalReferenceLink.build(sample, url, null);
	}

	static public ExternalReferenceLink build(String sample, 
			String url, 
			String id) {
		return new ExternalReferenceLink(sample, url, id);
	}
}
