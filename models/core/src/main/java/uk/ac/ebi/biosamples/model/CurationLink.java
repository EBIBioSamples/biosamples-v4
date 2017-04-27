package uk.ac.ebi.biosamples.model;

import java.util.Objects;

public class CurationLink implements Comparable<CurationLink> {

	private final Curation curation;
	private final String sample;
	
	
	private CurationLink(String sample, Curation curation) {
		this.sample = sample;
		this.curation = curation;
	}

	
	@Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof CurationLink)) {
            return false;
        }
        CurationLink other = (CurationLink) o;
        return Objects.equals(this.curation, other.curation)
        		&& Objects.equals(this.sample, other.sample);
    }
    
    @Override
    public int hashCode() {
    	return Objects.hash(sample, curation);
    }

	@Override
	public int compareTo(CurationLink other) {
		if (other == null) {
			return 1;
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
    	sb.append(this.curation);
    	sb.append(")");
    	return sb.toString();
    }

	
	public static CurationLink build(Curation curation, String sample) {
		return new CurationLink(sample,curation);
	}
}
