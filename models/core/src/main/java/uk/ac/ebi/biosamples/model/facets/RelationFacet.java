package uk.ac.ebi.biosamples.model.facets;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class RelationFacet extends StringListFacet {

    private RelationFacet(String label, long count, List attributeList) {
        super(label, count, attributeList);
    }

    @Override
    public FacetType getType() {
        return FacetType.OUTGOING_RELATIONSHIP;
    }

    @JsonCreator
    public static RelationFacet build(@JsonProperty String label, @JsonProperty long count, @JsonProperty("content") List attributeList) {
        return new RelationFacet(label, count, attributeList);
    }

}
