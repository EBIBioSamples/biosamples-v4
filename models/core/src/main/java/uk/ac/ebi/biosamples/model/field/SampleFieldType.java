package uk.ac.ebi.biosamples.model.field;

import uk.ac.ebi.biosamples.model.facets.FacetType;
import uk.ac.ebi.biosamples.model.filters.FilterType;

import java.util.Optional;

public enum SampleFieldType {
    ATTRIBUTE(FacetType.ATTRIBUTE, FilterType.ATTRIBUTE_FILTER),
    INVERSE_RELATION(FacetType.INCOMING_RELATIONSHIP, FilterType.INVERSE_RELATION_FILTER),
    RELATION(FacetType.OUTGOING_RELATIONSHIP, FilterType.RELATION_FILER),
    DOMAIN(null, FilterType.DOMAIN_FILTER),
    UPDATE_DATE(FacetType.DATE, FilterType.DATE_FILTER),
    RELEASE_DATE(FacetType.DATE, FilterType.DATE_FILTER);




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
}
