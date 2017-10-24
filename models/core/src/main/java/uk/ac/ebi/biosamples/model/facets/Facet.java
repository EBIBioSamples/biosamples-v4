package uk.ac.ebi.biosamples.model.facets;

import com.fasterxml.jackson.annotation.*;
import uk.ac.ebi.biosamples.model.FacetFilterFieldType;
import uk.ac.ebi.biosamples.model.facets.content.FacetContent;
import uk.ac.ebi.biosamples.model.filters.FilterType;

import java.util.Optional;

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
public interface Facet extends Comparable<Facet>{

    @JsonProperty("type")
    public FacetType getType();

    public String getLabel();

    public Long getCount();

    public FacetContent getContent();

    @JsonIgnore
    public default Optional<FilterType> getAssociatedFilterType() {
        return FacetFilterFieldType.getFilterForFacet(this.getType());
    }

    public interface Builder {
        Facet build();

        Builder withContent(FacetContent content);

    }

    default int compareTo(Facet otherFacet) {
        return Long.compare(this.getCount(),otherFacet.getCount());
    }

}
