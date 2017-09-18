package uk.ac.ebi.biosamples.model.facets;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.Collections;
import java.util.List;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NONE,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property="type",
        visible = true
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = AttributeFacet.class),
        @JsonSubTypes.Type(value = RelationFacet.class),
        @JsonSubTypes.Type(value = InverseRelationFacet.class)
})
public abstract class StringListFacet extends Facet {

    private List<LabelCountEntry> attributeList;

    protected StringListFacet(String label, long count, List<LabelCountEntry> attributeList) {
        super(label, count);
        this.attributeList = attributeList;
    }

    @Override
    public List<LabelCountEntry> getContent() {
        return Collections.unmodifiableList(this.attributeList);
    }

    @JsonCreator
    public static StringListFacet build(
            @JsonProperty("label") String label,
            @JsonProperty("count") long count,
            @JsonProperty("content") List content,
            @JsonProperty("type") String facetTypeName) {
        return FacetFactory.buildStringList(FacetType.ofFacetName(facetTypeName), label, count, content);
    }

}

