package uk.ac.ebi.biosamples.model.facets;

import java.util.List;

public class AttributeFacet extends StringListFacet {

    public AttributeFacet(String label, long count, List attributeList) {
        super(label, count, attributeList);
    }

    @Override
    public FacetType getType() {
        return FacetType.ATTRIBUTE;
    }
}
