package uk.ac.ebi.biosamples.model.facets;

import java.util.List;

public class InverseRelationFacet extends StringListFacet {

    public InverseRelationFacet(String label, long count, List attributeList) {
        super(label, count, attributeList);
    }

    @Override
    public FacetType getType() {
        return FacetType.INCOMING_RELATIONSHIP;
    }
}
