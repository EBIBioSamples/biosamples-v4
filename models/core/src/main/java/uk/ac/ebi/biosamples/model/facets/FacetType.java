package uk.ac.ebi.biosamples.model.facets;

import com.fasterxml.jackson.annotation.JsonValue;
import uk.ac.ebi.biosamples.model.filters.FilterType;

import java.util.HashMap;
import java.util.Map;

public enum FacetType {
    ATTRIBUTE("Attribute", FilterType.ATTRIBUTE_FILTER),
    INCOMING_RELATIONSHIP("Inverse Relation", FilterType.INVERSE_RELATION_FILTER),
    OUTGOING_RELATIONSHIP("Relation", FilterType.RELATION_FILER),
    DATE("Date", FilterType.DATE_FILTER);

    private static Map<String, FacetType> facetIdMap = new HashMap<>();

    static {
        for(FacetType type: values()) {
            facetIdMap.put(type.getFacetId(), type);
        }
    }

    private String facetId;
    private FilterType associatedFilterType;

    FacetType(String facetName, FilterType associatedFilter) {
        this.facetId = facetName;
        this.associatedFilterType = associatedFilter;
    }

    @JsonValue
    public String getFacetId() {
        return facetId.toLowerCase();
    }



}
