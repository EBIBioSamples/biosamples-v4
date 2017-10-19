package uk.ac.ebi.biosamples.model.facets;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.hateoas.core.Relation;

@Relation(collectionRelation = "facets")
public class InverseRelationFacet extends Facet {


    @JsonCreator
    public InverseRelationFacet(@JsonProperty("label") String label, @JsonProperty("count") long count, @JsonProperty("content") LabelCountListContent content) {
        super(label, count, content);
    }

    @Override
    public FacetType getType() {
        return FacetType.INCOMING_RELATIONSHIP;
    }

}
