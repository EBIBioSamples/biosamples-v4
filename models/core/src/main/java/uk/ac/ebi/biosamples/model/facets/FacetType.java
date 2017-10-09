package uk.ac.ebi.biosamples.model.facets;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import uk.ac.ebi.biosamples.model.filters.FilterType;

import java.util.HashMap;
import java.util.Map;

public enum FacetType {
//    UNKNOWN("", "", null),
    ATTRIBUTE("_av_ss","Attribute", FilterType.ATTRIBUTE_FILTER),
    INCOMING_RELATIONSHIP("_ir_ss", "Inverse Relation", FilterType.INVERSE_RELATION_FILTER),
    OUTGOING_RELATIONSHIP("_or_ss", "Relation", FilterType.RELATION_FILER),
    DATE("_dt", "Date", FilterType.DATE_FILTER);

    private static Map<String, FacetType> solrSuffixMap = new HashMap<>();
    private static Map<String, FacetType> facetIdMap = new HashMap<>();

    static {
        for(FacetType type: values()) {
            solrSuffixMap.put(type.getSolrSuffix(), type);
            facetIdMap.put(type.getFacetId(), type);
        }
    }

    private String solrSuffix;
    private String facetId;
    private FilterType associatedFilterType;

    FacetType(String solrSuffix, String facetName, FilterType associatedFilter) {
        this.solrSuffix = solrSuffix;
        this.facetId = facetName;
        this.associatedFilterType = associatedFilter;
    }

    public String getSolrSuffix() {
        return solrSuffix;
    }

    @JsonValue
    public String getFacetId() {
        return facetId.toLowerCase();
    }

    public FilterType getAssociatedFilterType() {
        return this.associatedFilterType;
    }

    public static FacetType ofField(String field) {
        for(FacetType type: values()) {
            if (field.endsWith(type.getSolrSuffix())) {
                return type;
            }
        }
        throw new IllegalArgumentException("Field " + field + " is not of a known type");
    }

    @JsonCreator
    public static FacetType ofFacetName(@JsonProperty String facetName) {
        for(FacetType type: values()) {
            if (facetName.toLowerCase().startsWith(type.getFacetId())) {
                return type;
            }
        }
        throw new IllegalArgumentException("Facet name " + facetName + " is not of a known type");
    }


}
