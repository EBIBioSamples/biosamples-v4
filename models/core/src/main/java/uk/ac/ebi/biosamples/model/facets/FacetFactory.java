package uk.ac.ebi.biosamples.model.facets;

import java.util.List;

public class FacetFactory {

    public static StringListFacet buildStringList(FacetType type, String label, long count, List<?> entries) {
        switch (type) {
            case ATTRIBUTE:
                return AttributeFacet.build(label, count, entries);
            case INCOMING_RELATIONSHIP:
                return InverseRelationFacet.build(label, count, entries);
            case OUTGOING_RELATIONSHIP:
                return RelationFacet.build(label, count, entries);
            default:
                throw new RuntimeException("Unsupported type " + type);
        }
    }
}
