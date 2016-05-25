package uk.ac.ebi.biosamples.models;

import java.time.LocalDate;
import java.util.Date;
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

	private String accession;
	private String name;
	private LocalDate releaseDate;
	private LocalDate updateDate;
	private Map<String, Set<String>> keyValues;
	private Map<String, Map<String, String>> ontologyTerms;
	private Map<String, Map<String, String>> units;

	private SimpleSample() {
	}

	@Override
	public String getAccession() {
		return accession;
	}

	@Override
	public String getName() {
		return name;
	}

	public LocalDate getReleaseDate() {
		return releaseDate;
	}

	@Override
	public LocalDate getUpdateDate() {
		return updateDate;
	}

	@Override
	public Set<String> getAttributeTypes() {
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
				&& Objects.equals(this.ontologyTerms, that.ontologyTerms)) {
			return true;
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, accession, updateDate, releaseDate, keyValues, units, ontologyTerms);
	}

	public static SimpleSample createFrom(Sample source) {
		SimpleSample sample = new SimpleSample();
		sample.accession = source.getAccession();
		sample.name = source.getName();
		sample.updateDate = source.getUpdateDate();
		sample.releaseDate = source.getReleaseDate();
		sample.keyValues = new HashMap<>();
		sample.units = new HashMap<>();
		sample.ontologyTerms = new HashMap<>();

		for (String type : source.getAttributeTypes()) {
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

	public static SimpleSample createFrom(String name, String accession, LocalDate updateDate, LocalDate releaseDate, Map<String, Set<String>> keyValues,
			Map<String, Map<String, String>> ontologyTerms, Map<String, Map<String, String>> units) {
		SimpleSample simpleSample = new SimpleSample();
		simpleSample.accession = accession;
		simpleSample.name = name;
		simpleSample.updateDate = updateDate;
		simpleSample.releaseDate = releaseDate;
		simpleSample.keyValues = keyValues;
		simpleSample.ontologyTerms = ontologyTerms;
		simpleSample.units = units;
		return simpleSample;
	}
}
