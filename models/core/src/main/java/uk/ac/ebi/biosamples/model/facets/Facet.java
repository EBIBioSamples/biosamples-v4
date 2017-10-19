package uk.ac.ebi.biosamples.model.facets;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = AttributeFacet.class, name="attribute"),
        @JsonSubTypes.Type(value = RelationFacet.class, name="relation"),
        @JsonSubTypes.Type(value = InverseRelationFacet.class, name="inverse relation")
})
//@JsonDeserialize(using = FacetDeserializer.class)
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

    @JsonProperty("type")
    public abstract FacetType getType();

    @Override
    public int compareTo(Facet o) {
        return Long.compare(this.getCount(), o.getCount());
    }

}
