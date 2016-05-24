package uk.ac.ebi.biosamples.models;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;

@Entity
public class JPASample implements Sample {

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private long id;

    @Column(name="ACCESSION", unique=true, nullable=false)
    private String accession;
    @Column(name="NAME", unique=false, nullable=false)
    private String name;
    @ManyToMany(cascade=CascadeType.PERSIST)
    private Set<JPAattribute> attributes;


	private JPASample() {
		super();
	}
	
	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
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
	public Set<String> getAttributeTypes() {
		Set<String> toReturn = new HashSet<String>();
		attributes.stream()
		.forEach(a -> toReturn.add(a.getType()));
		return toReturn;
	}

	@Override
	public Set<String> getAttributeValues(String type) {
		Set<String> toReturn = new HashSet<String>();
		attributes.stream()
		.filter(a -> a.getType().equals(type))
		.forEach(a -> toReturn.add(a.getValue()));
		return toReturn;
	}

	@Override
	public String getAttributeUnit(String type, String value) {
		Optional<JPAattribute> attr = attributes.stream()
				.filter(a -> a.getType().equals(type))
				.filter(a -> a.getValue().equals(value))
				.findFirst();
			if (attr.isPresent()) {
				return attr.get().getUnit();
			} else {
				return null;
			}
	}

	@Override
	public String getAttributeOntologyTerm(String type, String value) {
		Optional<JPAattribute> attr = attributes.stream()
			.filter(a -> a.getType().equals(type))
			.filter(a -> a.getValue().equals(value))
			.findFirst();
		if (attr.isPresent()) {
			return attr.get().getOntologyTerm();
		} else {
			return null;
		}
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
				&& Objects.equals(this.attributes, that.attributes)) {
			return true;
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, accession, attributes);
	}
	
	public static JPASample createFrom(Sample source) {
		JPASample sample = new JPASample();
		sample.accession = source.getAccession();
		sample.name = source.getName();

		sample.attributes = new HashSet<>();
		for (String type : source.getAttributeTypes()) {
			for (String value : source.getAttributeValues(type)) {
				JPAattribute attrib = new JPAattribute();
				attrib.setType(type);
				attrib.setValue(value);
				
				if (source.getAttributeUnit(type, value) != null) {
					attrib.setUnit(source.getAttributeUnit(type, value));
				}

				if (source.getAttributeOntologyTerm(type, value) != null) {
					attrib.setOntologyTerm(source.getAttributeOntologyTerm(type, value));
				}
				sample.attributes.add(attrib);
			}
		}

		return sample;
	}
}
