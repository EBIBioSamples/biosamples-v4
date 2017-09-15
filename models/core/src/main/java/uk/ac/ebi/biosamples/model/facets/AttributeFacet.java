package uk.ac.ebi.biosamples.model.facets;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class AttributeFacet extends StringListFacet {


    private AttributeFacet(String label, long count, List attributeList) {
        super(label, count, attributeList);
    }

    @Override
    public FacetType getType() {
        return FacetType.ATTRIBUTE;
    }

    @JsonCreator
    public static AttributeFacet build(@JsonProperty("label") String label, @JsonProperty("count") long count, @JsonProperty("content") List attributeList) {
        return new AttributeFacet(label, count, attributeList);
    }

}
