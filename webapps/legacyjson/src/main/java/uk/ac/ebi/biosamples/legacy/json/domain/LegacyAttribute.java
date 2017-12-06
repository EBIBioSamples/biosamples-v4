package uk.ac.ebi.biosamples.legacy.json.domain;

import java.util.ArrayList;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.google.common.collect.Lists;

import org.springframework.util.StringUtils;
import uk.ac.ebi.biosamples.model.Attribute;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonPropertyOrder({"text", "ontologyTerms"})
public class LegacyAttribute {

    private uk.ac.ebi.biosamples.model.Attribute attribute;

    public LegacyAttribute(uk.ac.ebi.biosamples.model.Attribute attribute) {
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
        	return attribute.getIri().toArray(new String[attribute.getIri().size()]);
        } else {
            return null;
        }
    }

    @JsonIgnore
    private boolean hasOntologyTerm() {
        return attribute.getIri().size() > 0;
    }
}

