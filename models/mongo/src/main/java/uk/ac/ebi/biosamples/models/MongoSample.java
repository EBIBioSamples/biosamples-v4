package uk.ac.ebi.biosamples.models;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import uk.ac.ebi.biosamples.models.Sample;

@JsonSerialize(using = MongoSampleSerializer.class)
@JsonDeserialize(using = MongoSampleDeserializer.class)
@Document
public class MongoSample implements Sample {

	@Id
	private String id;

	@Indexed
	protected String accession;

	protected String name;

	@Indexed
	protected LocalDateTime release;
	@Indexed
	protected LocalDateTime update;

	protected Map<String, Set<String>> keyValues = new HashMap<>();
	protected Map<String, Map<String, String>> ontologyTerms = new HashMap<>();
	protected Map<String, Map<String, String>> units = new HashMap<>();
	protected Map<String, Set<String>> relationships = new HashMap<>();

	private MongoSample() {
		super();
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
		return release;
	}

	@Override
	public LocalDateTime getUpdate() {
		return update;
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

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	@Override
	public boolean equals(Object other) {
		if (other == null)
			return false;
		if (other == this)
			return true;
		if (!(other instanceof MongoSample))
			return false;
		MongoSample that = (MongoSample) other;
		if (Objects.equals(this.name, that.name) && Objects.equals(this.accession, that.accession)
				&& Objects.equals(this.keyValues, that.keyValues) && Objects.equals(this.units, that.units)
				&& Objects.equals(this.ontologyTerms, that.ontologyTerms)) {
			return true;
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, accession, keyValues, units, ontologyTerms);
	}

	public static MongoSample createFrom(Sample source) {
		MongoSample sample = new MongoSample();
		sample.accession = source.getAccession();
		sample.name = source.getName();
		sample.update = source.getUpdate();
		sample.release = source.getRelease();
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

	public static MongoSample createFrom(String id, String name, String accession, LocalDateTime updateDate,
			LocalDateTime releaseDate, Map<String, Set<String>> keyValues,
			Map<String, Map<String, String>> ontologyTerms, Map<String, Map<String, String>> units,
			Map<String, Set<String>> relationships) {
		MongoSample sample = new MongoSample();
		sample.id = id;
		sample.name = name;
		sample.accession = accession;
		sample.update = updateDate;
		sample.release = releaseDate;
		sample.keyValues = keyValues;
		sample.ontologyTerms = ontologyTerms;
		sample.units = units;
		sample.relationships = relationships;
		return sample;
	}

}
