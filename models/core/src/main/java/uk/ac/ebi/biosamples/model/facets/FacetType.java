package uk.ac.ebi.biosamples.model.facets;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.HashMap;
import java.util.Map;

public enum FacetType {
    ATTRIBUTE("_av_ss","Attribute"),
    INCOMING_RELATIONSHIP("_ir_ss", "Inverse Relation"),
    OUTGOING_RELATIONSHIP("_or_ss", "Relation"),
    DATE("_dt", "Date");

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

    FacetType(String solrSuffix, String facetName) {
        this.solrSuffix = solrSuffix;
        this.facetId = facetName;
    }

    public String getSolrSuffix() {
        return solrSuffix;
    }

    @JsonValue
    public String getFacetId() {
        return facetId.toLowerCase();
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
