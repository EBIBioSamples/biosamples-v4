package uk.ac.ebi.biosamples.model.facets;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class InverseRelationFacet extends StringListFacet {

    private InverseRelationFacet(String label, long count, List attributeList) {
        super(label, count, attributeList);
    }

    @Override
    public FacetType getType() {
        return FacetType.INCOMING_RELATIONSHIP;
    }

    @JsonCreator
    public static InverseRelationFacet build(@JsonProperty("label") String label, @JsonProperty("count") long count, @JsonProperty("content") List attributeList) {
        return new InverseRelationFacet(label, count, attributeList);
    }
}
