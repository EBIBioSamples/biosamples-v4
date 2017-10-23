package uk.ac.ebi.biosamples.model.facets;

import com.fasterxml.jackson.annotation.*;
import uk.ac.ebi.biosamples.model.field.SampleFieldType;
import uk.ac.ebi.biosamples.model.filters.FilterType;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = AttributeFacet.class, name="attribute"),
        @JsonSubTypes.Type(value = RelationFacet.class, name="relation"),
        @JsonSubTypes.Type(value = InverseRelationFacet.class, name="inverse relation")
})
@JsonPropertyOrder(value = {"type", "label", "count", "content"})
public abstract class Facet implements Comparable<Facet>{

    private String label;
    private long count;
    private FacetContent content;

    protected Facet(String label, long count, FacetContent content) {
        this.label = label;
        this.count = count;
        this.content = content;
    }

    public String getLabel() {
        return label;
    }


    public long getCount() {
        return count;
    }

    public FacetContent getContent() {
        return this.content;
    }

    @JsonIgnore
    public abstract SampleFieldType getFieldType();

    @JsonProperty("type")
    public FacetType getType() {
        return this.getFieldType().getFacetType().get();
    }

    @JsonIgnore
    public FilterType getAssociatedFilterType() {
        return this.getFieldType().getFilterType().get();
    }


    @Override
    public int compareTo(Facet o) {
        return Long.compare(this.getCount(), o.getCount());
    }

}
