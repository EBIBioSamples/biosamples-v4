package uk.ac.ebi.biosamples.model.facets;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
public abstract class Facet implements Comparable<Facet> {
    private final String label;
    private final long count;

    // Active filters is an object that contains all the active filters of a facet
    // In the case of the label list, that is the list of the labels that ar active
    // Trying now to integrate the filters inside of the content
    // private final FacetFilter<T> activeFilters;

    protected Facet(String label, long count) {
        this.label = label;
        this.count = count;
    }

    public String getLabel() {
        return this.label;
    }

    public long getCount() {
        return count;
    }

    public abstract Object getContent();

    @JsonProperty(value = "type")
    public abstract FacetType getType();


    @Override
    public int compareTo(Facet o) {
        return Long.compare(this.getCount(), o.getCount());
    }
}
