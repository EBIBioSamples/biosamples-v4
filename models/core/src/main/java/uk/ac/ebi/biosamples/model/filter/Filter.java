package uk.ac.ebi.biosamples.model.filter;

import java.util.Optional;

import uk.ac.ebi.biosamples.model.FacetFilterFieldType;
import uk.ac.ebi.biosamples.model.facet.FacetType;

public interface Filter {

    public FilterType getType();

    /**
     * The label the filter is targeting
     * @return
     */
    public String getLabel();

    /**
     * Return the optional content of the filter. The content is optional because
     * can be used also to filter for samples having a specific characteristic
     * @return filter specific value, if available
     */
    public Optional<?> getContent();

    /**
     * Generate the serialized version of the filter usable through the web interface
     * @return string representing the filter value
     */
    public String getSerialization();

    /**
     * Get the facet associated to the filter, if any is available
     * @return optional facet type
     */
    public default Optional<FacetType> getAssociatedFacetType() {
        return FacetFilterFieldType.getFacetForFilter(this.getType());
    }

    public interface Builder {
        public Filter build();

        /**
         * Create a builder starting from a filter serialization
         * @param filterSerialized string representing a filter
         * @return a Builder to compose the filter
         */
        public Filter.Builder parseContent(String filterSerialized);

    }
}
