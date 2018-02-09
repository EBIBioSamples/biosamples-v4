package uk.ac.ebi.biosamples.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonRawValue;

import java.util.List;

/**
 * Object to represent the ld+json version of the @see <a href="http://schema.org/PropertyValue">Property Value</a>
 * in schema.org
 */

@JsonPropertyOrder({ "@type", "name", "value", "valueReference" })
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JsonLDPropertyValue {


    @JsonProperty("@type")
    private final String type = "PropertyValue";

    private String name;
    private String value;


//    @JsonProperty("valueReference")
//    private List<JsonLDStructuredValue> valueReference;

    private JsonLDCategoryCode valueReference;

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

    public JsonLDCategoryCode getValueReference() {
        return valueReference;
    }

    public void setValueReference(JsonLDCategoryCode valueReference) {
        this.valueReference = valueReference;
    }
}
