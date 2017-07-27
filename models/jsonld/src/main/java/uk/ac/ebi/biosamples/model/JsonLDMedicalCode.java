package uk.ac.ebi.biosamples.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * ld+json object for the MedicalCode in schema.org {@link}https://health-lifesci.schema.org/MedicalCode
 */
public class JsonLDMedicalCode {

    @JsonProperty("@context")
    private final String context = "http://schema.org";

    @JsonProperty("@type")
    private final String type = "MedicalCode";

    private String codeValue;
    private String codeSystem;

    public String getContext() {
        return context;
    }

    public String getType() {
        return type;
    }

    public String getCodeValue() {
        return codeValue;
    }

    public void setCodeValue(String codeValue) {
        this.codeValue = codeValue;
    }

    public String getCodeSystem() {
        return codeSystem;
    }

    public void setCodeSystem(String codeSystem) {
        this.codeSystem = codeSystem;
    }

}
