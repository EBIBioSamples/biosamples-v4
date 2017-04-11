package uk.ac.ebi.biosamples.model;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ExternalReference implements Comparable<ExternalReference> {
	
	private final String url;

	private ExternalReference(String url) {
		this.url = url;
	}

	public String getUrl() {
		return this.url;
	}
	
	@Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof ExternalReference)) {
            return false;
        }
        ExternalReference other = (ExternalReference) o;
        return Objects.equals(this.url, other.url);
    }
    
    @Override
    public int hashCode() {
    	return Objects.hash(url);
    }

	@Override
	public int compareTo(ExternalReference other) {
		if (other == null) {
			return 1;
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
    	sb.append(this.url);
    	sb.append(")");
    	return sb.toString();
    }

    @JsonCreator
    public static ExternalReference build(@JsonProperty("url") String url) {
		ExternalReference externalReference = new ExternalReference(url);
		return externalReference;
	}
}
