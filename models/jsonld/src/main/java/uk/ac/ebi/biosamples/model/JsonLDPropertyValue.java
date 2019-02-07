package uk.ac.ebi.biosamples.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.List;

/**
 * Object to represent the ld+json version of the @see <a href="http://schema.org/PropertyValue">Property Value</a>
 * in schema.org
 */

@JsonPropertyOrder({ "@type", "name", "value", "valueReference" })
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JsonLDPropertyValue implements BioschemasObject{


    @JsonProperty("@type")
    private final String type = "PropertyValue";

    private String name;
    private String value;
    private String unitText;
    private String unitCode;


//    @JsonProperty("valueReference")
//    private List<JsonLDStructuredValue> valueReference;

    private List<JsonLDDefinedTerm> valueReference;
    private List<JsonLDDefinedTerm> propertyId;

    public String getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() { return name; }

    public void setValue(String value) {
        this.value = value;
    }

    public List<JsonLDDefinedTerm> getValueReference() {
        return valueReference;
    }

    public void setValueReference(List<JsonLDDefinedTerm> valueReference) {
        this.valueReference = valueReference;
    }

    public String getUnitCode() {
        return unitCode;
    }

    public JsonLDPropertyValue unitCode(String unitCode) {
        this.unitCode = unitCode;
        return this;
    }

    public String getUnitText() {
        return unitText;
    }

    public JsonLDPropertyValue unitText(String unitText) {
        this.unitText = unitText;
        return this;
    }
}
