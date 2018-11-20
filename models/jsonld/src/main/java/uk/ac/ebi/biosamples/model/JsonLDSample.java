package uk.ac.ebi.biosamples.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.net.URI;
import java.util.List;

/**
 * Object representing BioSchema Sample entity
 */
@JsonPropertyOrder({ "@context", "@type", "additionalType", "@id", "identifier", "name", "description", "url", "dataset", "additionalProperty"})
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JsonLDSample implements BioschemasObject {

    private final URI sampleOntologyURI =  URI.create("http://purl.obolibrary.org/obo/OBI_0000747");

    @JsonProperty("@context")
    private final BioSchemasContext sampleContext;

    @JsonProperty("@type")
    private final String[] type = {"BioChemEntity", "Sample"};

    //private final String additionalType = "http://www.ontobee.org/ontology/OBI?iri=http://purl.obolibrary.org/obo/OBI_0000747";
    private String id;
    private String sameAs;
    private String[] identifiers;
    private String name;
    private String description;
    private String url;

    private List<String> dataset;

    @JsonProperty("additionalProperty")
    private List<JsonLDPropertyValue> additionalProperties;

    public JsonLDSample() {
        sampleContext = new BioSchemasContext();
        sampleContext.addContext("Sample", sampleOntologyURI);
    }

    @JsonIgnore
    public BioSchemasContext getContext() {
        return sampleContext;
    }

    public String[] getType() {
        return type;
    }

//    public String getAdditionalType() {
//        return additionalType;
//    }


    @JsonProperty("@id")
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @JsonProperty("sameAs")
    public String getSameAs() {
        return sameAs;
    }

    public void setSameAs(String sameAs) {
        this.sameAs = sameAs;
    }

    @JsonProperty("identifier")
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
