package uk.ac.ebi.biosamples.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({ "@type", "name", "url", "identifier" })
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JsonLDStructuredValue implements BioschemasObject{

    @JsonProperty("@type")
    private final String type = "StructuredValue";

    private String name;
    private String url;
    private String identifier;


    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
