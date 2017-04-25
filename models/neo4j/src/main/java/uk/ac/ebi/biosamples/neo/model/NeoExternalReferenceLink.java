package uk.ac.ebi.biosamples.neo.model;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.SortedSet;

import org.neo4j.ogm.annotation.EndNode;
import org.neo4j.ogm.annotation.GraphId;
import org.neo4j.ogm.annotation.Index;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Property;
import org.neo4j.ogm.annotation.Relationship;
import org.neo4j.ogm.annotation.RelationshipEntity;
import org.neo4j.ogm.annotation.StartNode;
import org.neo4j.ogm.annotation.typeconversion.Convert;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import com.google.common.collect.ComparisonChain;
import com.google.common.hash.Hashing;

import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.neo.service.LocalDateTimeConverter;

@NodeEntity(label = "ExternalReferenceLink")
public class NeoExternalReferenceLink implements Comparable<NeoExternalReferenceLink> {

	@GraphId
	private Long id;

    @Relationship(type = "HAS_EXTERNAL_REFERENCE_SOURCE")
	private NeoSample sample;

    @Relationship(type = "HAS_EXTERNAL_REFERENCE_TARGET")
	private NeoExternalReference externalReference;


	@Property
	@Index(unique=true, primary=true)	
	private String hash;
    
	
	private NeoExternalReferenceLink() {	}
	
	private NeoExternalReferenceLink(NeoSample sample, NeoExternalReference neoExternalReference, String hash) {
		this.sample = sample;
		this.externalReference = neoExternalReference;
		this.hash = hash;
	}

	public NeoSample getSample() {
		return sample;
	}

	public NeoExternalReference getExternalReference() {
		return externalReference;
	}
	
	public String getHash() {
		return hash;
	}

	@Override
    public boolean equals(Object o) {

        if (o == this) return true;
        if (!(o instanceof NeoExternalReferenceLink)) {
            return false;
        }
        NeoExternalReferenceLink other = (NeoExternalReferenceLink) o;
        
        String thisSampleAccession = null;
        if (this.sample != null) thisSampleAccession = this.sample.getAccession();       
        String otherSampleAccession = null;
        if (other.sample != null) otherSampleAccession = other.sample.getAccession();
        
        String thisExternalReference = null;
        if (this.externalReference != null) thisExternalReference = this.externalReference.getUrl();
        String otherExternalReference = null;
        if (other.externalReference != null) otherExternalReference = other.externalReference.getUrl();
        
        return Objects.equals(thisSampleAccession, otherSampleAccession) 
        		&& Objects.equals(thisExternalReference, otherExternalReference);
    }
    
    @Override
    public int hashCode() {
        
        String thisSampleAccession = null;
        if (this.sample != null) thisSampleAccession = this.sample.getAccession();       
        
        String thisExternalReference = null;
        if (this.externalReference != null) thisExternalReference = this.externalReference.getUrl();
        
        return Objects.hash(thisSampleAccession, thisExternalReference);
    }	

	@Override
	public int compareTo(NeoExternalReferenceLink other) {		
		if (other == null) {
			return 1;
		}
        
        String thisSampleAccession = null;
        if (this.sample != null) thisSampleAccession = this.sample.getAccession();       
        String otherSampleAccession = null;
        if (other.sample != null) otherSampleAccession = other.sample.getAccession();
        
        String thisExternalReference = null;
        if (this.externalReference != null) thisExternalReference = this.externalReference.getUrl();
        String otherExternalReference = null;
        if (other.externalReference != null) otherExternalReference = other.externalReference.getUrl();
		
        if (thisSampleAccession == null && otherSampleAccession != null) {
        	return -1;
        } else if (thisSampleAccession != null && otherSampleAccession == null) {
        	return 1;
        } else if (thisSampleAccession != null && otherSampleAccession != null && !thisSampleAccession.equals(otherSampleAccession)) {
			return thisSampleAccession.compareTo(otherSampleAccession);
		}

        if (thisExternalReference == null && otherExternalReference != null) {
        	return -1;
        } else if (thisExternalReference != null && otherExternalReference == null) {
        	return 1;
        } else if (thisExternalReference != null && otherExternalReference != null && !thisExternalReference.equals(otherExternalReference)) {
			return thisExternalReference.compareTo(otherExternalReference);
		}
		
		return 0;
	}

	public static NeoExternalReferenceLink build(NeoSample sample, NeoExternalReference externalReference) {

    	String hash = Hashing.sha256().newHasher()
			.putUnencodedChars(externalReference.getUrl())
			.putUnencodedChars(sample.getAccession())
			.hash().toString();
    	
    	//TODO bake user id into hash
    	
    	NeoExternalReferenceLink neoExternalReferenceApplication = new NeoExternalReferenceLink(sample, externalReference, hash);
		return neoExternalReferenceApplication;
	}
}
