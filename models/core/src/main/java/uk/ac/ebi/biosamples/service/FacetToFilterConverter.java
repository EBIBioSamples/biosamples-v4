package uk.ac.ebi.biosamples.service;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.model.facets.FacetType;
import uk.ac.ebi.biosamples.model.filters.FilterType;

@Service
public class FacetToFilterConverter implements Converter<FilterType, FacetType>  {

    @Override
    public FacetType convert(FilterType source) {
        switch(source) {
            case ATTRIBUTE_FILTER:
                return FacetType.ATTRIBUTE;
            case RELATION_FILER:
                return FacetType.OUTGOING_RELATIONSHIP;
            case INVERSE_RELATION_FILTER:
                return FacetType.INCOMING_RELATIONSHIP;
            case DATE_FILTER:
                return FacetType.DATE;
            default:
                return null;
        }
    }

    public FilterType convert(FacetType source) {
       switch(source) {
           case ATTRIBUTE:
               return FilterType.ATTRIBUTE_FILTER;
           case OUTGOING_RELATIONSHIP:
               return FilterType.RELATION_FILER;
           case INCOMING_RELATIONSHIP:
               return FilterType.INVERSE_RELATION_FILTER;
           default:
               return null;
       }
    }
}
