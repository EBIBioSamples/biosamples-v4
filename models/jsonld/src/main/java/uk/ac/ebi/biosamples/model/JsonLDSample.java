package uk.ac.ebi.biosamples.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.List;

/**
 * Object representing BioSchema Sample entity
 */
@JsonPropertyOrder({ "@context", "@type", "additionalType", "identifier", "name", "description", "url", "dataset", "additionalProperty"})
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JsonLDSample implements BioschemasObject {

    @JsonProperty("@context")
    private final String context = "http://schema.org";

    @JsonProperty("@type")
    private final String[] type = {"BioChemEntity", "Sample"};

    //private final String additionalType = "http://www.ontobee.org/ontology/OBI?iri=http://purl.obolibrary.org/obo/OBI_0000747";
    private String[] identifiers;
    private String name;
    private String description;
    private String url;

    private List<String> dataset;

    @JsonProperty("additionalProperty")
    private List<JsonLDPropertyValue> additionalProperties;

    public String getContext() {
        return context;
    }

    public String[] getType() {
        return type;
    }

//    public String getAdditionalType() {
//        return additionalType;
//    }

    public String[] getIdentifiers() {
        return identifiers;
    }

    public void setIdentifiers(String[] identifiers) {
        this.identifiers = identifiers;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public List<String> getDataset() {
        return dataset;
    }

    public void setDataset(List<String> dataset) {
        this.dataset = dataset;
    }

    public List<JsonLDPropertyValue> getAdditionalProperties() {
        return additionalProperties;
    }

    public void setAdditionalProperties(List<JsonLDPropertyValue> additionalProperties) {
        this.additionalProperties = additionalProperties;
    }


}
