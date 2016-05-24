package uk.ac.ebi.biosamples.models;

import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

/**
 * A simple implementation of the Sample interface suitable for use in testing
 * 
 * @author faulcon
 *
 */
@JsonSerialize(using = SampleSerializer.class)
@JsonDeserialize(using = SampleDeserializer.class)
public class SimpleSample implements Sample {
	
	protected String accession;
	protected String name;
	protected Map<String, Set<String>> keyValues;
	protected Map<String, Map<String, URI>> ontologyTerms;
	protected Map<String, Map<String, String>> units;
	
    protected SimpleSample() {}
        
	@Override
	public String getAccession() {
		return accession;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public ImmutableSet<String> getAttributeTypes() {
		return ImmutableSet.copyOf(keyValues.keySet());
	}

	@Override
	public ImmutableSet<String> getAttributeValues(String type) {
		return ImmutableSet.copyOf(keyValues.get(type));
	}

	@Override
	public Optional<String> getAttributeUnit(String type, String value) {
		if (!units.containsKey(type)) {
			return Optional.empty();
		}
		if (!units.get(type).containsKey(value)) {
			return Optional.empty();
		}
		return Optional.of(units.get(type).get(value));
	}

	@Override
	public Optional<URI> getAttributeOntologyTerm(String type, String value) {
		if (!ontologyTerms.containsKey(type)) {
			return Optional.empty();
		}
		if (!ontologyTerms.get(type).containsKey(value)) {
			return Optional.empty();
		}
		return Optional.of(ontologyTerms.get(type).get(value));
	}
	
	@Override
    public boolean equals(Object other) {
    	if (other == null) return false;
    	if (other == this) return true;
    	if (!(other instanceof SimpleSample)) return false;
    	SimpleSample that = (SimpleSample) other;
    	if (Objects.equal(this.name, that.name)
    			&& Objects.equal(this.accession, that.accession)
    			&& Objects.equal(this.keyValues, that.keyValues)
    			&& Objects.equal(this.units, that.units)
    			&& Objects.equal(this.ontologyTerms, that.ontologyTerms)) {
    		return true;
    	} else {
    		return false;
    	}
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name, accession, keyValues, units, ontologyTerms);
    }
	
	public static SimpleSample createFrom(Sample source) {
		SimpleSample sample = new SimpleSample();
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
	
	public static SimpleSample createFrom(String name, String accession, 
			Map<String, Set<String>> keyValues, Map<String, Map<String, URI>> ontologyTerms, Map<String, Map<String, String>> units) {
		SimpleSample simpleSample = new SimpleSample();
		simpleSample.accession = accession;
		simpleSample.name = name;
		simpleSample.keyValues = ImmutableMap.copyOf(keyValues);
		simpleSample.ontologyTerms = ImmutableMap.copyOf(ontologyTerms);
		simpleSample.units = ImmutableMap.copyOf(units);
		return simpleSample;
	}
}
