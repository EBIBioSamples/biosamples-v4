package uk.ac.ebi.biosamples.neo.model;

import java.util.Objects;

import org.neo4j.ogm.annotation.GraphId;
import org.neo4j.ogm.annotation.Index;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Property;
import org.neo4j.ogm.annotation.Relationship;
import com.google.common.hash.Hashing;

@NodeEntity(label = "CurationLink")
public class NeoCurationLink implements Comparable<NeoCurationLink> {

	@GraphId
	private Long id;

    @Relationship(type = "HAS_CURATION_SOURCE")
	private NeoSample sample;

    @Relationship(type = "HAS_CURATION_TARGET")
	private NeoCuration curation;
    
    @Property
    private String domain;


	@Property
	@Index(unique=true, primary=true)	
	private String hash;

	private NeoCurationLink() {
	};

	public Long getId() {
		return id;
	}

	public NeoCuration getCuration() {
		return curation;
	}

	public NeoSample getSample() {
		return sample;
	}
	
	public String getDomain() {
		return domain;
	}
	
	public String getHash() {
		return hash;
	}

	@Override
    public boolean equals(Object o) {

        if (o == this) return true;
        if (!(o instanceof NeoCurationLink)) {
            return false;
        }
        NeoCurationLink other = (NeoCurationLink) o;
        
        String thisSampleAccession = null;
        if (this.sample != null) thisSampleAccession = this.sample.getAccession();       
        String otherSampleAccession = null;
        if (other.sample != null) otherSampleAccession = other.sample.getAccession();
        
        String thisCuration = null;
        if (this.curation != null) thisCuration = this.curation.getHash();
        String otherCuration = null;
        if (other.curation != null) otherCuration = other.curation.getHash();
        
        return Objects.equals(this.getDomain(), other.getDomain())
        		&& Objects.equals(thisSampleAccession, otherSampleAccession) 
        		&& Objects.equals(thisCuration, otherCuration);
    }
    
    @Override
    public int hashCode() {
        
        String thisSampleAccession = null;
        if (this.sample != null) thisSampleAccession = this.sample.getAccession();       
        
        String thisCuration = null;
        if (this.curation != null) thisCuration = this.curation.getHash();
        
        return Objects.hash(thisSampleAccession, thisCuration);
    }	

	@Override
	public int compareTo(NeoCurationLink other) {		
		if (other == null) {
			return 1;
		}
        
        String thisSampleAccession = null;
        if (this.sample != null) thisSampleAccession = this.sample.getAccession();       
        String otherSampleAccession = null;
        if (other.sample != null) otherSampleAccession = other.sample.getAccession();
        
        String thisCuration = null;
        if (this.curation != null) thisCuration = this.curation.getHash();
        String otherCuration = null;
        if (other.curation != null) otherCuration = other.curation.getHash();
		
        if (thisSampleAccession == null && otherSampleAccession != null) {
        	return -1;
        } else if (thisSampleAccession != null && otherSampleAccession == null) {
        	return 1;
        } else if (thisSampleAccession != null && otherSampleAccession != null && !thisSampleAccession.equals(otherSampleAccession)) {
			return thisSampleAccession.compareTo(otherSampleAccession);
		}

        if (thisCuration == null && otherCuration != null) {
        	return -1;
        } else if (thisCuration != null && otherCuration == null) {
        	return 1;
        } else if (thisCuration != null && otherCuration != null && !thisCuration.equals(otherCuration)) {
			return thisCuration.compareTo(otherCuration);
		}
		
		return 0;
	}
	
	public static NeoCurationLink build(NeoCuration curation, NeoSample sample, String domain) {
		NeoCurationLink newRelationship = new NeoCurationLink();
		newRelationship.curation = curation;
		newRelationship.sample = sample;
		newRelationship.domain = domain;
		

		newRelationship.hash = Hashing.sha256().newHasher()
			.putUnencodedChars(sample.getAccession())
			.putUnencodedChars(domain)
			.putUnencodedChars(curation.getHash())
			.hash().toString();
    			
		return newRelationship;
	}
}
