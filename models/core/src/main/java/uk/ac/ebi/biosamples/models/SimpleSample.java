package uk.ac.ebi.biosamples.models;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

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
	protected LocalDateTime releaseDate;
	protected LocalDateTime updateDate;
	protected Map<String, Set<String>> keyValues = new HashMap<>();
	protected Map<String, Map<String, String>> ontologyTerms = new HashMap<>();
	protected Map<String, Map<String, String>> units = new HashMap<>();
	protected Map<String, Set<String>> relationships = new HashMap<>();

	protected SimpleSample() {
	}

	@Override
	public String getAccession() {
		return accession;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public LocalDateTime getRelease() {
		return releaseDate;
	}

	@Override
	public LocalDateTime getUpdate() {
		return updateDate;
	}

	@Override
	public Set<String> getAttributeKeys() {
		return keyValues.keySet();
	}

	@Override
	public Set<String> getAttributeValues(String type) {
		return keyValues.get(type);
	}

	@Override
	public String getAttributeUnit(String type, String value) {
		if (!units.containsKey(type)) {
			return null;
		}
		if (!units.get(type).containsKey(value)) {
			return null;
		}
		return units.get(type).get(value);
	}

	@Override
	public String getAttributeOntologyTerm(String type, String value) {
		if (!ontologyTerms.containsKey(type)) {
			return null;
		}
		if (!ontologyTerms.get(type).containsKey(value)) {
			return null;
		}
		return ontologyTerms.get(type).get(value);
	}

	@Override
	public Set<String> getRelationshipTypes() {
		return relationships.keySet();
	}

	@Override
	public Set<String> getRelationshipTargets(String type) {
		return relationships.get(type);
	}

	@Override
	public boolean equals(Object other) {
		if (other == null)
			return false;
		if (other == this)
			return true;
		if (!(other instanceof SimpleSample))
			return false;
		SimpleSample that = (SimpleSample) other;
		if (Objects.equals(this.name, that.name) && Objects.equals(this.accession, that.accession)
				&& Objects.equals(this.updateDate, that.updateDate)
				&& Objects.equals(this.releaseDate, that.releaseDate)
				&& Objects.equals(this.keyValues, that.keyValues) && Objects.equals(this.units, that.units)
				&& Objects.equals(this.ontologyTerms, that.ontologyTerms)
				&& Objects.equals(this.relationships, that.relationships)) {
			return true;
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, accession, updateDate, releaseDate, keyValues, units, ontologyTerms, relationships);
	}

	public static SimpleSample createFrom(Sample source) {
		SimpleSample sample = new SimpleSample();
		sample.accession = source.getAccession();
		sample.name = source.getName();
		sample.updateDate = source.getUpdate();
		sample.releaseDate = source.getRelease();
		sample.keyValues = new HashMap<>();
		sample.units = new HashMap<>();
		sample.ontologyTerms = new HashMap<>();

		for (String type : source.getAttributeKeys()) {
			sample.keyValues.put(type, new HashSet<>());
			for (String value : source.getAttributeValues(type)) {
				sample.keyValues.get(type).add(value);

				if (source.getAttributeUnit(type, value) != null) {
					if (!sample.units.containsKey(type)) {
						sample.units.put(type, new HashMap<>());
					}
					sample.units.get(type).put(value, source.getAttributeUnit(type, value));
				}

				if (source.getAttributeOntologyTerm(type, value) != null) {
					if (!sample.ontologyTerms.containsKey(type)) {
						sample.ontologyTerms.put(type, new HashMap<>());
					}
					sample.ontologyTerms.get(type).put(value, source.getAttributeOntologyTerm(type, value));
				}
			}
		}

		return sample;
	}

	public static SimpleSample createFrom(String name, String accession, LocalDateTime updateDate, LocalDateTime releaseDate, Map<String, Set<String>> keyValues,
			Map<String, Map<String, String>> ontologyTerms, Map<String, Map<String, String>> units, Map<String, Set<String>> relationships) {
		SimpleSample sample = new SimpleSample();
		sample.accession = accession;
		sample.name = name;
		sample.updateDate = updateDate;
		sample.releaseDate = releaseDate;
		sample.keyValues = keyValues;
		sample.ontologyTerms = ontologyTerms;
		sample.units = units;
		sample.relationships = relationships;
		return sample;
	}
}
