package uk.ac.ebi.biosamples.model.field;

import uk.ac.ebi.biosamples.model.facets.FacetType;
import uk.ac.ebi.biosamples.model.filters.FilterType;

import java.util.EnumMap;
import java.util.Optional;

public enum SampleFieldType {
    ATTRIBUTE(FacetType.ATTRIBUTE, FilterType.ATTRIBUTE_FILTER),
    INVERSE_RELATION(FacetType.INCOMING_RELATIONSHIP, FilterType.INVERSE_RELATION_FILTER),
    RELATION(FacetType.OUTGOING_RELATIONSHIP, FilterType.RELATION_FILER),
    DOMAIN(null, FilterType.DOMAIN_FILTER),
    UPDATE_DATE(FacetType.DATE, FilterType.DATE_FILTER),
    RELEASE_DATE(FacetType.DATE, FilterType.DATE_FILTER);

    private static EnumMap<FacetType, SampleFieldType> facetToField = new EnumMap<>(FacetType.class);
    private static EnumMap<FilterType, SampleFieldType> filterToField = new EnumMap<>(FilterType.class);

    static {
        for(SampleFieldType fieldType: values()) {
            if(fieldType.getFacetType().isPresent()) {
                facetToField.put(fieldType.getFacetType().get(), fieldType);
            }

            if(fieldType.getFilterType().isPresent()) {
                filterToField.put(fieldType.getFilterType().get(), fieldType);
            }
        }
    }


    private FacetType facetType;
    private FilterType filterType;

    SampleFieldType(FacetType facetType, FilterType filterType) {
        this.facetType = facetType;
        this.filterType = filterType;
    }

    public Optional<FacetType> getFacetType() {
        return Optional.ofNullable(facetType);
    }

    public Optional<FilterType> getFilterType() {
        return Optional.ofNullable(filterType);
    }

    public static SampleFieldType getFieldForFacet(FacetType facetType) {
        SampleFieldType fieldType = facetToField.get(facetType);
        if (fieldType == null) {
            throw new RuntimeException("No field is associated with the facet type " + facetType);
        }
        return fieldType;
    }

    public static SampleFieldType getFieldForFilter(FilterType filterType) {
        SampleFieldType fieldType = filterToField.get(filterType);
        if (fieldType == null) {
            throw new RuntimeException("No field is associated with the filter type " + filterType);
        }
        return fieldType;

    }

    public static Optional<FilterType> getFilterForFacet(FacetType facetType) {
        return getFieldForFacet(facetType).getFilterType();
    }

    public static Optional<FacetType> getFacetForFilter(FilterType filterType) {
        return getFieldForFilter(filterType).getFacetType();
    }

}
