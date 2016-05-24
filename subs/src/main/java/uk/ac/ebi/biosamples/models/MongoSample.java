package uk.ac.ebi.biosamples.models;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Optional;

import org.springframework.data.annotation.Id;

import uk.ac.ebi.biosamples.models.Sample;

public class MongoSample extends SimpleSample {
	
    @Id
    private String id;
    
	private Optional<String> previousAccession;

    private MongoSample() {
    	super();
	}
     
    public String getId() {
    	return id;
    }
    
	public Optional<String> getPreviousAccession() {
		return previousAccession;
	}
	
	public void doArchive() {
		//can only do this if we currently have an accession
		if (accession != null) {
			this.previousAccession = Optional.of(accession);
			this.accession = null;
		}
	}
	
	public static MongoSample createFrom(Sample source) {
		MongoSample sample = new MongoSample();
		sample.accession = source.getAccession();
		sample.name = source.getName();
		sample.keyValues = new HashMap<>();
		sample.units = new HashMap<>();
		sample.ontologyTerms = new HashMap<>();		
		
		for (String type : source.getAttributeTypes()) {
			sample.keyValues.put(type, new HashSet<>());
			for (String value : source.getAttributeValues(type)) {
				sample.keyValues.get(type).add(value);
				
				if (source.getAttributeUnit(type, value).isPresent()) {
					if (!sample.units.containsKey(type)) {
						sample.units.put(type, new HashMap<>());
					}
					sample.units.get(type).put(value, source.getAttributeUnit(type, value).get());
				}
				
				if (source.getAttributeOntologyTerm(type, value).isPresent()) {
					if (!sample.ontologyTerms.containsKey(type)) {
						sample.ontologyTerms.put(type, new HashMap<>());
					}
					sample.ontologyTerms.get(type).put(value, source.getAttributeOntologyTerm(type, value).get());
				}
			}
		}
		
		return sample;
	}

}
