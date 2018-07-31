package uk.ac.ebi.biosamples.model;

import java.util.EnumMap;
import java.util.Optional;

import uk.ac.ebi.biosamples.model.facet.FacetType;
import uk.ac.ebi.biosamples.model.filter.FilterType;

/**
 * Purpose of this class is to bind Facet and filters together, so is possible to
 * move from one to the other in a predictable way. Every time a new Filter/Facet is created,
 * a correspondent value should be added here to bind them together
 */
public enum FacetFilterFieldType {
    ATTRIBUTE(FacetType.ATTRIBUTE_FACET, FilterType.ATTRIBUTE_FILTER),
    INVERSE_RELATION(FacetType.INVERSE_RELATION_FACET, FilterType.INVERSE_RELATION_FILTER),
    RELATION(FacetType.RELATION_FACET, FilterType.RELATION_FILER),
    DOMAIN(null, FilterType.DOMAIN_FILTER),
    UPDATE_DATE(null, FilterType.DATE_FILTER),
    RELEASE_DATE(null, FilterType.DATE_FILTER),
    EXTERNAL_REFERENCE_DATA(FacetType.EXTERNAL_REFERENCE_DATA_FACET, FilterType.EXTERNAL_REFERENCE_DATA_FILTER ), 
    NAME(null, FilterType.NAME_FILTER),
    ACCESSION(null, FilterType.ACCESSION_FILTER),
    DATA_TYPE(FacetType.DATA_TYPE, FilterType.DATA_TYPE_FILTER);

    private static EnumMap<FacetType, FacetFilterFieldType> facetToField = new EnumMap<>(FacetType.class);
    private static EnumMap<FilterType, FacetFilterFieldType> filterToField = new EnumMap<>(FilterType.class);

    static {
        for(FacetFilterFieldType fieldType: values()) {
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

    FacetFilterFieldType(FacetType facetType, FilterType filterType) {
        this.facetType = facetType;
        this.filterType = filterType;
    }

    /**
     * Return an optional facet type if any is associated with the provided fieldType
     * @return Optional facet type
     */
    public Optional<FacetType> getFacetType() {
        return Optional.ofNullable(facetType);
    }

    /**
     * Return an optional filter type if any is associated with the provided fieldType
     * @return Optional filter type
     */
    public Optional<FilterType> getFilterType() {
        return Optional.ofNullable(filterType);
    }

    public static FacetFilterFieldType getFieldForFacet(FacetType facetType) {
        FacetFilterFieldType fieldType = facetToField.get(facetType);
        if (fieldType == null) {
            throw new RuntimeException("No field is associated with the facet type " + facetType);
        }
        return fieldType;
    }

    public static FacetFilterFieldType getFieldForFilter(FilterType filterType) {
        FacetFilterFieldType fieldType = filterToField.get(filterType);
        if (fieldType == null) {
            throw new RuntimeException("No field is associated with the filter type " + filterType);
        }
        return fieldType;

    }

    /**
     * Return the filter type associated with the facet type, if is available
     * @param facetType the type of facet
     * @return Optional filter type
     */
    public static Optional<FilterType> getFilterForFacet(FacetType facetType) {
        return getFieldForFacet(facetType).getFilterType();
    }

    /**
     * Return the facet type associated with the filter type, if is available
     * @param filterType the type of filter
     * @return Optional facet type
     */
    public static Optional<FacetType> getFacetForFilter(FilterType filterType) {
        return getFieldForFilter(filterType).getFacetType();
    }

}
