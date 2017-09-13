package uk.ac.ebi.biosamples.model.facets;

import java.util.HashMap;
import java.util.Map;

public enum FacetType {
    ATTRIBUTE("_av_ss","Attribute"),
    INCOMING_RELATIONSHIP("_ir_ss", "Inverse Relation"),
    OUTGOING_RELATIONSHIP("_or_ss", "Relation");
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

    public String getFacetId() {
        return facetId;
    }

    public static FacetType ofField(String field) {
        for(FacetType type: values()) {
            if (field.endsWith(type.getSolrSuffix())) {
                return type;
            }
        }
        throw new IllegalArgumentException("Field " + field + " is not of a known type");
    }

    public static FacetType ofFacetName(String facetName) {
        for(FacetType type: values()) {
            if (facetName.startsWith(type.getFacetId())) {
                return type;
            }
        }
        throw new IllegalArgumentException("Facet name " + facetName + " is not of a known type");
    }

}
