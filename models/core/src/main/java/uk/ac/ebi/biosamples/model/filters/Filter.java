package uk.ac.ebi.biosamples.model.filters;

import uk.ac.ebi.biosamples.model.FacetFilterFieldType;
import uk.ac.ebi.biosamples.model.facets.FacetType;

import java.util.Optional;

public interface Filter {

    public FilterType getType();

    public String getLabel();

    public Optional<?> getContent();

    public String getSerialization();

    public default Optional<FacetType> getAssociatedFacetType() {
        return FacetFilterFieldType.getFacetForFilter(this.getType());
    }

    public interface Builder {
        public Filter build();

        public Filter.Builder parseContent(String filterSerialized);

    }
}
