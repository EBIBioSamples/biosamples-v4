package uk.ac.ebi.biosamples.solr.model;

import java.util.HashMap;
import java.util.Map;

public enum FacetType {
    ATTRIBUTE("_av_ss","(Attribute) "),
    INCOMING_RELATIONSHIP("_ir_ss", "(Relation rev.) "),
    OUTGOING_RELATIONSHIP("_or_ss", "(Relation) ");
    private static Map<String, FacetType> suffixToType = new HashMap<>();
    private static Map<String, FacetType> prefixToType = new HashMap<>();

    static {
        for(FacetType type: values()) {
            suffixToType.put(type.getFieldSuffix(), type);
            prefixToType.put(type.getFacetNamePrefix(), type);
        }
    }

    private String fieldSuffix;
    private String facetNamePrefix;

    FacetType(String fieldSuffix, String namePrefix) {
        this.fieldSuffix = fieldSuffix;
        this.facetNamePrefix = namePrefix;
    }

    public String getFieldSuffix() {
        return fieldSuffix;
    }

    public String getFacetNamePrefix() {
        return facetNamePrefix;
    }

    public static FacetType ofField(String field) {
        for(FacetType type: values()) {
            if (field.endsWith(type.getFieldSuffix())) {
                return type;
            }
        }
        throw new IllegalArgumentException("Field " + field + " is not of a known type");
    }

    public static FacetType ofFacetName(String facetName) {
        for(FacetType type: values()) {
            if (facetName.startsWith(type.getFacetNamePrefix())) {
                return type;
            }
        }
        throw new IllegalArgumentException("Facet name " + facetName + " is not of a known type");
    }

}
