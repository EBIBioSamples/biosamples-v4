package uk.ac.ebi.biosamples.model.facet;

import java.lang.reflect.InvocationTargetException;

import com.fasterxml.jackson.annotation.JsonValue;

public enum FacetType {
    ATTRIBUTE_FACET("attribute", AttributeFacet.Builder.class),
    INVERSE_RELATION_FACET("inverse relation", InverseRelationFacet.Builder.class),
    RELATION_FACET("relation", RelationFacet.Builder.class),
    EXTERNAL_REFERENCE_DATA_FACET("external reference data", ExternalReferenceDataFacet.Builder.class);

    private String name;
    private Class<? extends Facet.Builder> associatedClass;

    FacetType(String name, Class<? extends Facet.Builder> associatedClass) {
        this.name = name;
        this.associatedClass = associatedClass;
    }

    public Facet.Builder getBuilderForLabelAndCount(String facetLabel, Long facetCount) {
        try {
            return this.associatedClass.getConstructor(String.class, Long.class)
                    .newInstance(facetLabel, facetCount);
        } catch (NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
            throw new RuntimeException("FacetType " + this + " does not provide a proper builder");
        }
    }


    @JsonValue
    public String getFacetName() {
        return this.name.toLowerCase();
    }


}
