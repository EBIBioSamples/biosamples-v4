package uk.ac.ebi.biosamples.model.facets;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.springframework.hateoas.core.Relation;
import uk.ac.ebi.biosamples.model.facets.content.FacetContent;
import uk.ac.ebi.biosamples.model.facets.content.LabelCountListContent;

@Relation(collectionRelation = "facets")
@JsonDeserialize(builder =  AttributeFacet.Builder.class)
public class AttributeFacet implements Facet {

    private String facetLabel;
    private Long facetCount;
    private LabelCountListContent content;

    private AttributeFacet(String facetLabel, Long facetCount, LabelCountListContent content) {
        this.facetLabel = facetLabel;
        this.facetCount = facetCount;
        this.content = content;
    }

    @Override
    public FacetType getType() {
        return FacetType.ATTRIBUTE_FACET;
    }

    @Override
    public String getLabel() {
        return this.facetLabel;
    }

    @Override
    public Long getCount() {
        return this.facetCount;
    }

    @Override
    public LabelCountListContent getContent() {
        return this.content;
    }

    public static class Builder implements Facet.Builder {

        private String field;
        private Long count;
        private LabelCountListContent content = null;

        @JsonCreator
        public Builder(@JsonProperty("label") String field,
                       @JsonProperty("count") Long count) {
            this.field = field;
            this.count = count;
        }

        @JsonProperty
        @Override
        public Builder withContent(FacetContent content) {
            if (!(content instanceof LabelCountListContent)) {
                throw new RuntimeException("Content not compatible with the facet");
            }

            this.content = (LabelCountListContent) content;
//            this.content = content;
            return this;
        }

        @Override
        public Facet build() {
            return new AttributeFacet(this.field, this.count, this.content);
        }


    }
}
