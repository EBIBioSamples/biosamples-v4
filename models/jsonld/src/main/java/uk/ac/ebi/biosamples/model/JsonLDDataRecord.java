package uk.ac.ebi.biosamples.model;

import com.fasterxml.jackson.annotation.*;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder(value = {"@context",  "@type", "identifier", "dateCreated", "dateModified", "mainEntity", "isPartOf"})
public class JsonLDDataRecord implements BioschemasObject{

    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @JsonProperty("@context")
    private final String context = "http://schema.org";

    @JsonProperty("@type")
    private final String type = "DataRecord";

    @JsonProperty("identifier")
    private String idetifier;

    @JsonProperty("mainEntity")
    private JsonLDSample mainEntity;

    private ZonedDateTime dateCreated;

    @JsonProperty("dateModified")
    private ZonedDateTime dateModified;

    @JsonProperty("isPartOf")
    private Map partOf = getDatasetPartOf();

    public String getIdetifier() {
        return idetifier;
    }

    public JsonLDDataRecord idetifier(String idetifier) {
        this.idetifier = idetifier;
        return this;
    }

    public JsonLDSample getMainEntity() {
        return mainEntity;
    }

    public JsonLDDataRecord mainEntity(JsonLDSample mainEntity) {
        this.mainEntity = mainEntity;
        return this;
    }

//    public ZonedDateTime getDateModified() {
//        return dateModified;
//    }

//    public ZonedDateTime getDateCreated() {
//        return dateCreated;
//    }

    @JsonGetter("dateModified")
    public String getDateModified() {
        return dateTimeFormatter.format(dateModified);
    }

    @JsonGetter("dateCreated")
    public String getDateCreated() {
        return dateTimeFormatter.format(dateCreated);
    }

    @JsonSetter("dateCreated")
    public JsonLDDataRecord dateCreated(String dateCreated) {
        this.dateCreated = LocalDateTime.parse(dateCreated, dateTimeFormatter).atZone(ZoneId.systemDefault());
        return this;
    }

    @JsonSetter("dateModified")
    public JsonLDDataRecord dateModified(String dateModified) {
        this.dateModified = LocalDateTime.parse(dateModified, dateTimeFormatter).atZone(ZoneId.systemDefault());
        return this;
    }

    public JsonLDDataRecord dateModified(ZonedDateTime dateModified) {
        this.dateModified = dateModified;
        return this;
    }


    public JsonLDDataRecord dateCreated(ZonedDateTime dateCreated) {
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
