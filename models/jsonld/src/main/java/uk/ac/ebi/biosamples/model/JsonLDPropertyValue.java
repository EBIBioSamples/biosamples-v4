package uk.ac.ebi.biosamples.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class JsonLDPropertyValue {

    @JsonProperty("@context")
    private final String context = "http://schema.org";

    @JsonProperty("@type")
    private final String type = "PropertyValue";

    private String propertyId;
    private String value;
    private JsonLDMedicalCode code;

    public String getContext() {
        return context;
    }

    public String getType() {
        return type;
    }

    public String getPropertyId() {
        return propertyId;
    }

    public void setPropertyId(String propertyId) {
        this.propertyId = propertyId;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public JsonLDMedicalCode getCode() {
        return code;
    }

    public void setCode(JsonLDMedicalCode code) {
        this.code = code;
    }
}
