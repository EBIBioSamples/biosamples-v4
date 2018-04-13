package uk.ac.ebi.biosamples.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;


@JsonPropertyOrder({ "@type", "name", "url", "codeValue" })
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JsonLDCategoryCode implements BioschemasObject {

    @JsonProperty("@type")
    private final String type = "CategoryCode";

    private String name;
    private String url;
    private String codeValue;

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }

    public String getCodeValue() {
        return codeValue;
    }

    public void setCodeValue(String codeValue) {
        this.codeValue = codeValue;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setUrl(String url) {
        this.url = url;
    }



}
