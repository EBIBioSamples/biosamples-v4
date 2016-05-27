package uk.ac.ebi.biosamples.models;

import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Table(uniqueConstraints = @UniqueConstraint(columnNames = { "KEY", "VALUE", "UNIT", "ONTOLOGYTERM" }) ) 
//cant create unique key on blobs in MySQL
@Entity
public class JPAAttribute {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "ID", unique = true, nullable = false)
	private long id;

	@Column(name = "KEY", unique = false, nullable = false)
	private String key;
	@Column(name = "VALUE", unique = false, nullable = false)
	@Lob
	private String value;
	@Column(name = "UNIT", unique = false, nullable = true)
	private String unit;
	@Column(name = "ONTOLOGYTERM", unique = false, nullable = true)
	private String ontologyTerm;


	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}
	
	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public String getUnit() {
		return unit;
	}

	public void setUnit(String unit) {
		this.unit = unit;
	}

	public String getOntologyTerm() {
		return ontologyTerm;
	}

	public void setOntologyTerm(String ontologyTerm) {
		this.ontologyTerm = ontologyTerm;
	}

	@Override
	public boolean equals(Object other) {
		if (other == null)
			return false;
		if (other == this)
			return true;
		if (!(other instanceof SimpleSample))
			return false;
		JPAAttribute that = (JPAAttribute) other;
		if (Objects.equals(this.key, that.value) && Objects.equals(this.unit, that.unit)
				&& Objects.equals(this.ontologyTerm, that.ontologyTerm)) {
			return true;
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return Objects.hash(key, value, unit, ontologyTerm);
	}

}
