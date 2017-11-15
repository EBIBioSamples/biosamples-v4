package uk.ac.ebi.biosamples.model;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Objects;

import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Attribute implements Comparable<Attribute> {

	private String type;
	private String value;
	private String iri;
	private String unit;
	
	private Attribute(){
		
	}
	
	@JsonProperty("type") 
    public String getType() {
		return type;
	}

	@JsonProperty("value") 
	public String getValue() {
		return value;
	}

	@JsonProperty("iri") 
	public String getIri() {
		return iri;
	}
	
	/**
	 * This returns a string representation of the URL to lookup the associated ontology term iri in
	 * EBI OLS. 
	 * @return
	 */
	@JsonIgnore
	public String getIriOls() {
		//TODO move this to service layer
		if (iri == null) return null;
		
		//check this is a sane iri
		UriComponents iriComponents = UriComponentsBuilder.fromUriString(iri).build(true);
		if (iriComponents.getScheme() == null
				|| iriComponents.getHost() == null 
				|| iriComponents.getPath() == null) {
			//incomplete iri (e.g. 9606, EFO_12345) don't bother to check
			return null;
		}
		
		try {
			return "http://www.ebi.ac.uk/ols/terms?iri="+URLEncoder.encode(iri.toString(), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			//should never get here
			throw new RuntimeException(e);
		}		
			
	}
	
	@JsonProperty("unit") 
	public String getUnit() {
		return unit;
	}

	@Override
    public boolean equals(Object o) {

        if (o == this) return true;
        if (!(o instanceof Attribute)) {
            return false;
        }
        Attribute other = (Attribute) o;
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
	public int compareTo(Attribute other) {
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
    	sb.append("Attribute(");
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
    
	static public Attribute build(String type, String value) {
		return build(type, value, null, null);
	}
	
    @JsonCreator
	static public Attribute build(@JsonProperty("type") String type, @JsonProperty("value") String value, 
			@JsonProperty("iri") String iri, @JsonProperty("unit") String unit) {
    	//cleanup inputs
    	if (type != null) type = type.trim();
    	if (value != null) value = value.trim();
    	if (iri != null) iri = iri.trim();
    	if (unit != null) unit = unit.trim();
    	//create output
		Attribute attr = new Attribute();
		attr.type = type;
		attr.value = value;
		attr.iri = iri;
		attr.unit = unit;
		return attr;
	}
}
