package uk.ac.ebi.biosamples.model;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;

public class CurationLink implements Comparable<CurationLink> {

	private final Curation curation;
	private final String sample;
	private final String domain;
	private final String hash;
	
	
	private CurationLink(String sample, String domain, Curation curation, String hash) {
		this.sample = sample;
		this.domain = domain;
		this.curation = curation;
		this.hash = hash;
	}
	
	public String getSample() {
		return sample;
	}
	
	public String getDomain() {
		return domain;
	}
	
	public Curation getCuration() {
		return curation;
	}
	
	public String getHash() {
		return hash;
	}

	
	@Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof CurationLink)) {
            return false;
        }
        CurationLink other = (CurationLink) o;
        return Objects.equals(this.curation, other.curation)
        		&& Objects.equals(this.sample, other.sample)
        		&& Objects.equals(this.domain, other.domain);
    }
    
    @Override
    public int hashCode() {
    	return Objects.hash(sample, domain, curation);
    }

	@Override
	public int compareTo(CurationLink other) {
		if (other == null) {
			return 1;
		}

		if (!this.domain.equals(other.domain)) {
			return this.domain.compareTo(other.domain);
		}
		if (!this.sample.equals(other.sample)) {
			return this.sample.compareTo(other.sample);
		}
		if (!this.curation.equals(other.curation)) {
			return this.curation.compareTo(other.curation);
		}
		return 0;
	}	

    @Override
    public String toString() {
    	StringBuilder sb = new StringBuilder();
    	sb.append("CurationLink(");
    	sb.append(this.sample);
    	sb.append(",");
    	sb.append(this.domain);
    	sb.append(",");
    	sb.append(this.curation);
    	sb.append(")");
    	return sb.toString();
    }

    //Used for deserializtion (JSON -> Java)
    @JsonCreator
	public static CurationLink build(@JsonProperty("sample") String sample, @JsonProperty("domain") String domain, @JsonProperty("curation") Curation curation) {

    	
    	Hasher hasher = Hashing.sha256().newHasher();
    	
    	if (sample != null) {
    		hasher.putUnencodedChars(sample);
    	}
		if (domain != null) {
			hasher.putUnencodedChars(domain);
		}
		hasher.putUnencodedChars(curation.getHash());

    	String hash = hasher.hash().toString();
    	
		return new CurationLink(sample,domain, curation, hash);
	}
}
