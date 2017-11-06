package uk.ac.ebi.biosamples.model;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonPropertyOrder({"text", "ontologyTerms"})
public class LegacyAttribute {

    private Attribute attribute;

    public LegacyAttribute(Attribute attribute) {
        this.attribute = attribute;
    }

    @JsonGetter
    public String text() {
        return attribute.getValue();
    }

    public String type() {
        return attribute.getType();
    }

    @JsonGetter
    public String[] ontologyTerms() {
        if (hasOntologyTerm()) {
            return new String[]{attribute.getIri()};
        } else {
            return null;
        }
    }

    @JsonIgnore
    private boolean hasOntologyTerm() {
        return attribute.getIri() != null && !attribute.getIri().isEmpty();
    }
}

