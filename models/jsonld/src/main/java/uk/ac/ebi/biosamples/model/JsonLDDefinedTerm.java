package uk.ac.ebi.biosamples.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;


@JsonPropertyOrder({ "@id", "@type", "name", "inDefinedTermSet", "termCode" })
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JsonLDDefinedTerm implements BioschemasObject {

    @JsonProperty("@type")
    private final String type = "DefinedTerm";

    @JsonProperty("@id")
    private String id;
    private String name;
    private String inDefinedTermSet;
    private String termCode;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public String getInDefinedTermSet() {
        return inDefinedTermSet;
    }

    public String getTermCode() {
        return termCode;
    }

    public void setTermCode(String termCode) {
        this.termCode = termCode;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setInDefinedTermSet(String inDefinedTermSet) {
        this.inDefinedTermSet = inDefinedTermSet;
    }



}
