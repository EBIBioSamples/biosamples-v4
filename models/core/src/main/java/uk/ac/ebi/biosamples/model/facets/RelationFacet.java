package uk.ac.ebi.biosamples.model.facets;

import java.util.List;

public class RelationFacet extends StringListFacet {

    public RelationFacet(String label, long count, List attributeList) {
        super(label, count, attributeList);
    }

    @Override
    public FacetType getType() {
        return FacetType.OUTGOING_RELATIONSHIP;
    }
}
