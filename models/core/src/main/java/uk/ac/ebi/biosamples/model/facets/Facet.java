package uk.ac.ebi.biosamples.model.facets;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * The use of this facet class is experimental. The idea is to make facet more generic and not just restricted
 * to list of string. Facet rendering should be left to Thymeleaf, and the IdentifiableFacetContent interface should integrate
 * methods to make the facet identifiable (using ids or string o else later). We also decided to use Generics instead of an
 * abstract class
 * @param <T>
 */
// TODO Facet need to be sortable by the FacetBuilder
public class Facet<T> implements Comparable<Facet<T>> {
    private final String label;
    private final long count;
    private final FacetType type;
    private final T content;

    // Active filters is an object that contains all the active filters of a facet
    // In the case of the label list, that is the list of the labels that ar active
    // Trying now to integrate the filters inside of the content
    // private final FacetFilter<T> activeFilters;

    protected Facet(String label, long count, FacetType type, T content) {
        this.label = label;
        this.count = count;
        this.type = type;
        this.content = content;
    }

    public String getLabel() {
        return this.label;
    }

    public long getCount() {
        return count;
    }

    public T getContent() {
        return this.content;
    }

    @JsonProperty(value = "type")
    public FacetType getType() {
        return this.type;
    }


    @Override
    public int compareTo(Facet<T> o) {
        return Long.compare(this.getCount(), o.getCount());
    }

    @JsonCreator
    public static Facet<List<LabelCountEntry>> build(
            @JsonProperty("type") FacetType facetType,
            @JsonProperty("label") String label,
            @JsonProperty("count") long count,
            @JsonProperty("content") List<LabelCountEntry> content) {

        switch (facetType) {
            case ATTRIBUTE:
            case INCOMING_RELATIONSHIP:
            case OUTGOING_RELATIONSHIP:
                return new Facet<>(label, count, facetType, content);
            default:
                throw new RuntimeException("Unsupported type " + facetType);
        }
    }
}
