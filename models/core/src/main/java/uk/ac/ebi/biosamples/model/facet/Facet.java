package uk.ac.ebi.biosamples.model.facet;

import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import uk.ac.ebi.biosamples.model.FacetFilterFieldType;
import uk.ac.ebi.biosamples.model.facet.content.FacetContent;
import uk.ac.ebi.biosamples.model.filter.FilterType;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = AttributeFacet.class, name="attribute"),
        @JsonSubTypes.Type(value = RelationFacet.class, name="relation"),
        @JsonSubTypes.Type(value = InverseRelationFacet.class, name="inverse relation"),
        @JsonSubTypes.Type(value = ExternalReferenceDataFacet.class, name="external reference data")
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

    /*
     * Builder interface to build Facets
     */
    public interface Builder {
        Facet build();

        Builder withContent(FacetContent content);

    }

    default int compareTo(Facet otherFacet) {
        return Long.compare(this.getCount(),otherFacet.getCount());
    }

}
