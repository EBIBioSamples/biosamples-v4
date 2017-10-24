package uk.ac.ebi.biosamples.service;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.model.facets.FacetType;
import uk.ac.ebi.biosamples.model.FacetFilterFieldType;
import uk.ac.ebi.biosamples.model.filters.FilterType;

@Service
public class FacetToFilterConverter implements Converter<FilterType, FacetType>  {

    @Override
    public FacetType convert(FilterType source) {
        return FacetFilterFieldType.getFacetForFilter(source).orElse(null);
    }

    public FilterType convert(FacetType source) {
        return FacetFilterFieldType.getFilterForFacet(source).orElse(null);
    }
}
