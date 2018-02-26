package uk.ac.ebi.biosamples.model.facet;

import org.springframework.hateoas.core.Relation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import uk.ac.ebi.biosamples.model.facet.content.FacetContent;
import uk.ac.ebi.biosamples.model.facet.content.LabelCountListContent;

@Relation(collectionRelation = "facets")
@JsonDeserialize(builder = RelationFacet.Builder.class)
public class RelationFacet implements Facet {

    private String facetLabel;
    private Long facetCount;
    private LabelCountListContent content;

    private RelationFacet(String facetLabel, Long facetCount, LabelCountListContent content) {
        this.facetLabel = facetLabel;
        this.facetCount = facetCount;
        this.content = content;
    }


    @Override
    public FacetType getType() {
        return FacetType.RELATION_FACET;
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
    
    @Override
    public String toString() {
    	StringBuilder sb = new StringBuilder();
    	sb.append("RelationFacet(");
    	sb.append(facetLabel);
    	sb.append(",");
    	sb.append(facetCount);
    	sb.append(",");
    	sb.append(content);
    	sb.append(")");
    	return sb.toString();
    }


    public static class Builder implements Facet.Builder {

        private String label;
        private Long count;
        private LabelCountListContent content = null;

        @JsonCreator
        public Builder(@JsonProperty("label") String label,
                       @JsonProperty("count") Long count) {
            this.label = label;
            this.count = count;
        }

        @JsonProperty
        @Override
        public Builder withContent(FacetContent content) {
            if (!(content instanceof LabelCountListContent)) {
                throw new RuntimeException("Content not compatible with a relation facet");
            }

            this.content = (LabelCountListContent) content;
            return this;
        }

        @Override
        public Facet build() {
            return new RelationFacet(this.label, this.count, this.content);
        }
    }
}

