package uk.ac.ebi.biosamples.models;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Table(uniqueConstraints = @UniqueConstraint(columnNames = { "ACCESSION" }) ) 
@Entity
public class JPASample implements Sample {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "ID", unique = true, nullable = false)
	private long id;

	@Column(name = "ACCESSION", unique = true, nullable = false)
	private String accession;
	@Column(name = "NAME", unique = false, nullable = false)
	private String name;
	@Column(name = "RELEASEDATE", unique = false, nullable = false)
	private LocalDate releaseDate;
	@Column(name = "UPDATEDATE", unique = false, nullable = false)
	private LocalDate updateDate;

	@ManyToMany  
	private Set<JPAAttribute> attributes;

	@ManyToMany  
	private Set<JPARelationship> relationships;

	private JPASample() {
		super();
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public Set<JPAAttribute> getAttributes() {
		return attributes;
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
	public Set<String> getAttributeKeys() {
		Set<String> toReturn = new HashSet<String>();
		attributes.stream().forEach(a -> toReturn.add(a.getKey()));
		return toReturn;
	}

	@Override
	public Set<String> getAttributeValues(String key) {
		Set<String> toReturn = new HashSet<String>();
		attributes.stream().filter(a -> a.getKey().equals(key)).forEach(a -> toReturn.add(a.getValue()));
		return toReturn;
	}

	@Override
	public String getAttributeUnit(String key, String value) {
		Optional<JPAAttribute> attr = attributes.stream().filter(a -> a.getKey().equals(key))
				.filter(a -> a.getValue().equals(value)).findFirst();
		if (attr.isPresent()) {
			return attr.get().getUnit();
		} else {
			return null;
		}
	}

	@Override
	public String getAttributeOntologyTerm(String key, String value) {
		Optional<JPAAttribute> attr = attributes.stream().filter(a -> a.getKey().equals(key))
				.filter(a -> a.getValue().equals(value)).findFirst();
		if (attr.isPresent()) {
			return attr.get().getOntologyTerm();
		} else {
			return null;
		}
	}

	@Override
	public Set<String> getRelationshipTypes() {
		Set<String> toReturn = new HashSet<String>();
		relationships.stream().forEach(r -> toReturn.add(r.getType()));
		return toReturn;
	}

	@Override
	public Set<String> getRelationshipTargets(String type) {
		Set<String> toReturn = new HashSet<String>();
		relationships.stream().filter(r -> r.getType().equals(type)).forEach(r -> toReturn.add(r.getTarget()));
		return toReturn;
	}

	@Override
	public boolean equals(Object other) {
		if (other == null)
			return false;
		if (other == this)
			return true;
		if (!(other instanceof SimpleSample))
			return false;
		JPASample that = (JPASample) other;
		if (Objects.equals(this.name, that.name) && Objects.equals(this.accession, that.accession)
				&& Objects.equals(this.attributes, that.attributes)
				&& Objects.equals(this.relationships, that.relationships)) {
			return true;
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, accession, attributes, relationships);
	}

	public static JPASample createFrom(Sample source) {
		JPASample sample = new JPASample();
		sample.accession = source.getAccession();
		sample.name = source.getName();
		//limit name length to 255 characters so it fits in db
		if (sample.name.length() > 255) {
			sample.name = sample.name.substring(0, 252)+"...";
		}
		sample.updateDate = source.getUpdateDate();
		sample.releaseDate = source.getReleaseDate();

		sample.attributes = new HashSet<>();
		for (String type : source.getAttributeKeys()) {
			for (String value : source.getAttributeValues(type)) {
				
				String unit = source.getAttributeUnit(type, value);
				String ontologyTerm = source.getAttributeOntologyTerm(type, value);
				
				//limit type length to 255 characters so it fits in db
				if (type.length() > 255) {
					type = type.substring(0, 252)+"...";
				}
				//limit unit length to 255 characters so it fits in db
				if (unit != null && unit.length() > 255) {
					unit = unit.substring(0, 252)+"...";
				}
				//limit ontologyTerm length to 255 characters so it fits in db
				if (ontologyTerm != null && ontologyTerm.length() > 255) {
					ontologyTerm = ontologyTerm.substring(0, 252)+"...";
				}
				
				JPAAttribute attrib = new JPAAttribute();
				attrib.setKey(type);
				attrib.setValue(value);

				if (unit != null) {
					attrib.setUnit(unit);
				}

				if (ontologyTerm != null) {
					attrib.setOntologyTerm(ontologyTerm);
				}
				sample.attributes.add(attrib);
			}
		}

		return sample;
	}
}
