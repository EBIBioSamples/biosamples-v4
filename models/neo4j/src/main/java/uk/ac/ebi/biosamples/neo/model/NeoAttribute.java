package uk.ac.ebi.biosamples.neo.model;

import java.util.Objects;

import org.neo4j.ogm.annotation.GraphId;
import org.neo4j.ogm.annotation.Index;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Property;

import uk.ac.ebi.biosamples.model.Attribute;

@NodeEntity(label = "Attribute")
public class NeoAttribute implements Comparable<NeoAttribute> {

	@GraphId
	private Long id;

	@Property
	@Index
	private String type;

	@Property
	@Index
	private String value;

	@Property
	@Index
	private String iri;

	@Property
	@Index
	private String unit;
	
	@Property
	@Index(unique=true, primary=true)
	private String compositeIdentifier;
	
	
	private NeoAttribute() {
		
	}

	public String getType() {
		return type;
	}

	public String getValue() {
		return value;
	}

	public String getIri() {
		return iri;
	}

	public String getUnit() {
		return unit;
	}
	@Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof NeoAttribute)) {
            return false;
        }
        NeoAttribute other = (NeoAttribute) o;
        return Objects.equals(this.type, other.type) 
        		&& Objects.equals(this.value, other.value)
        		&& Objects.equals(this.iri, other.iri)
        		&& Objects.equals(this.unit, other.unit);
    }
    
    @Override
    public int hashCode() {
    	return Objects.hash(type, value, iri, unit);
    }

	@Override
	public int compareTo(NeoAttribute other) {
		if (other == null) {
			return 1;
		}
		
		if (!this.type.equals(other.type)) {
			return this.type.compareTo(other.type);
		}

		if (!this.value.equals(other.value)) {
			return this.value.compareTo(other.value);
		}
		
		if (this.iri == null && other.iri != null) {
			return -1;
		}
		if (this.iri != null && other.iri == null) {
			return 1;
		}
		if (this.iri != null && other.iri != null 
				&& !this.iri.equals(other.iri)) {
			return this.iri.compareTo(other.iri);
		}

		
		if (this.unit == null && other.unit != null) {
			return -1;
		}
		if (this.unit != null && other.unit == null) {
			return 1;
		}
		if (this.unit != null && other.unit != null 
				&& !this.unit.equals(other.unit)) {
			return this.unit.compareTo(other.unit);
		}
		
		return 0;
	}

    @Override
    public String toString() {
    	StringBuilder sb = new StringBuilder();
    	sb.append("NeoAttribute(");
    	sb.append(type);
    	sb.append(",");
    	sb.append(value);
    	sb.append(",");
    	sb.append(iri);
    	sb.append(",");
    	sb.append(unit);
    	sb.append(")");
    	return sb.toString();
    }

	
	public static NeoAttribute build(String type, String value, String iri, String unit) {
		NeoAttribute neoAttribute = new NeoAttribute();
		neoAttribute.type = type;
		neoAttribute.value = value;
		neoAttribute.iri = iri;
		neoAttribute.unit = unit;
		
		//build the composite identifier here so that neo4j can use it as a primary index
		//to unique and merge nodes
		String compositeIdentifier = type+"_"+value;
		if (iri != null)  {
			compositeIdentifier = compositeIdentifier+"_"+iri;
		}
		if (unit != null)  {
			compositeIdentifier = compositeIdentifier+"_"+unit;
		}
		neoAttribute.compositeIdentifier = compositeIdentifier;
		return neoAttribute;
	}
}
