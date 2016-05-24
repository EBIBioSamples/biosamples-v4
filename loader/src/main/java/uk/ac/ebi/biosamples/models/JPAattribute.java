package uk.ac.ebi.biosamples.models;

import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Table(uniqueConstraints = @UniqueConstraint(columnNames = { "TYPE", "VALUE", "UNIT", "ONTOLOGYTERM" }) )
@Entity
public class JPAattribute {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private long id;

	@Column(name = "TYPE", unique = false, nullable = false)
	private String type;
	@Column(name = "VALUE", unique = false, nullable = false)
	private String value;
	@Column(name = "UNIT", unique = false, nullable = true)
	private String unit;
	@Column(name = "ONTOLOGYTERM", unique = false, nullable = true)
	private String ontologyTerm;

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
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
		JPAattribute that = (JPAattribute) other;
		if (Objects.equals(this.type, that.value) && Objects.equals(this.unit, that.unit)
				&& Objects.equals(this.ontologyTerm, that.ontologyTerm)) {
			return true;
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return Objects.hash(type, value, unit, ontologyTerm);
	}

}
