package uk.ac.ebi.biosamples.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder(value = {"@context",  "@type", "identifier", "dateCreated", "dateModified", "mainEntity", "isPartOf"})
public class JsonLDRecord implements BioschemasObject{

    @JsonProperty("@context")
    private final String context = "http://schema.org";

    @JsonProperty("@type")
    private final String type = "Record";

    @JsonProperty("identifier")
    private String idetifier;

    @JsonProperty("mainEntity")
    private JsonLDSample mainEntity;

    @JsonProperty("dateCreated")
    private ZonedDateTime dateCreated;

    @JsonProperty("dateModified")
    private ZonedDateTime dateModified;

    @JsonProperty("isPartOf")
    private Map partOf = getDatasetPartOf();

    public String getIdetifier() {
        return idetifier;
    }

    public JsonLDRecord idetifier(String idetifier) {
        this.idetifier = idetifier;
        return this;
    }

    public JsonLDSample getMainEntity() {
        return mainEntity;
    }

    public JsonLDRecord mainEntity(JsonLDSample mainEntity) {
        this.mainEntity = mainEntity;
        return this;
    }

    public ZonedDateTime getDateModified() {
        return dateModified;
    }

    public JsonLDRecord dateModified(ZonedDateTime dateModified) {
        this.dateModified = dateModified;
        return this;
    }

    public ZonedDateTime getDateCreated() {
        return dateCreated;
    }

    public JsonLDRecord dateCreated(ZonedDateTime dateCreated) {
        this.dateCreated = dateCreated;
        return this;
    }


    public Map getDatasetPartOf() {
        Map<String, String> datasetPartOf = new HashMap<>();
        datasetPartOf.put("@type", "Dataset");
        //TODO Use relative application url and not hard-coded one
        datasetPartOf.put("@id", "https://www.ebi.ac.uk/biosamples/samples");
        return datasetPartOf;
    }

}
