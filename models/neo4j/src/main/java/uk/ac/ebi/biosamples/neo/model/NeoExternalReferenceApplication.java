package uk.ac.ebi.biosamples.neo.model;

import java.time.LocalDateTime;
import java.util.Objects;

import org.neo4j.ogm.annotation.EndNode;
import org.neo4j.ogm.annotation.GraphId;
import org.neo4j.ogm.annotation.Property;
import org.neo4j.ogm.annotation.RelationshipEntity;
import org.neo4j.ogm.annotation.StartNode;
import org.neo4j.ogm.annotation.typeconversion.Convert;

import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.neo.service.LocalDateTimeConverter;

@RelationshipEntity(type = "HAS_EXTERNAL_REFERENCE")
public class NeoExternalReferenceApplication implements Comparable<NeoExternalReferenceApplication> {

	@GraphId
	private Long id;
	
	@StartNode
	private NeoSample sample;
	@EndNode
	private NeoExternalReference externalReference;


	@Convert(LocalDateTimeConverter.class)
	@Property
	private LocalDateTime time;
	
	private NeoExternalReferenceApplication() {
		
	}

	public NeoSample getSample() {
		return sample;
	}

	public NeoExternalReference getExternalReference() {
		return externalReference;
	}

	public LocalDateTime getTime() {
		return time;
	}

	@Override
    public boolean equals(Object o) {

        if (o == this) return true;
        if (!(o instanceof NeoExternalReferenceApplication)) {
            return false;
        }
        NeoExternalReferenceApplication other = (NeoExternalReferenceApplication) o;
        return Objects.equals(this.sample.getAccession(), other.sample.getAccession()) 
        		&& Objects.equals(this.externalReference.getUrl(), other.externalReference.getUrl())
        		&& Objects.equals(this.time, other.time);
    }
    
    @Override
    public int hashCode() {
    	return Objects.hash(sample.getAccession(), externalReference.getUrl(), time);
    }	

	@Override
	public int compareTo(NeoExternalReferenceApplication other) {
		if (other == null) {
			return 1;
		}
		
		if (!this.sample.getAccession().equals(other.sample.getAccession())) {
			return this.sample.getAccession().compareTo(other.sample.getAccession());
		}

		if (!this.externalReference.getUrl().equals(other.externalReference.getUrl())) {
			return this.externalReference.getUrl().compareTo(other.externalReference.getUrl());
		}
		
		return 0;
	}
	
	public static NeoExternalReferenceApplication build(NeoSample sample, NeoExternalReference externalReference) {
		NeoExternalReferenceApplication neoExternalReferenceApplication = new NeoExternalReferenceApplication();
		neoExternalReferenceApplication.sample = sample;
		neoExternalReferenceApplication.externalReference = externalReference;
		return neoExternalReferenceApplication;
	}
}
