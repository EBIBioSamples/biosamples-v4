package uk.ac.ebi.biosamples.model.facets;

import java.util.List;

public class FacetFactory {


    public static StringListFacet buildStringList(FacetType type, String label, long count, List<?> entries) {
        switch (type) {
            case ATTRIBUTE:
                return new AttributeFacet(label, count, entries);
            case INCOMING_RELATIONSHIP:
                return new InverseRelationFacet(label, count, entries);
            case OUTGOING_RELATIONSHIP:
                return new RelationFacet(label, count, entries);
            default:
                throw new RuntimeException("Unsupported type " + type);
        }
    }
}
