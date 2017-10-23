package uk.ac.ebi.biosamples.model.facets;

import com.fasterxml.jackson.annotation.JsonValue;

public enum FacetType {
    ATTRIBUTE_FACET,
    INVERSE_RELATION_FACET,
    RELATION_FACET,
    DATE_FACET;


    @JsonValue
    public String getFacetId() {
        return name()
                .replaceFirst("_FACET$","")
                .replaceAll("_", " ")
                .toLowerCase();
    }



}
