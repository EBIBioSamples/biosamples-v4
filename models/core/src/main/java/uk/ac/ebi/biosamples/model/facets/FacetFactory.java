package uk.ac.ebi.biosamples.model.facets;

public class FacetFactory {

    public static Facet build( FacetType facetType, String label, long count, FacetContent rawContent) {

        switch(facetType) {
            case INCOMING_RELATIONSHIP:
                if (!(LabelCountListContent.class.isAssignableFrom(rawContent.getClass()))) {
                    throw new RuntimeException("Content not compatible with " + facetType.name() + " facet type");
                }
                return new InverseRelationFacet(label, count, (LabelCountListContent) rawContent);
            case OUTGOING_RELATIONSHIP:
                if (!(LabelCountListContent.class.isAssignableFrom(rawContent.getClass()))) {
                    throw new RuntimeException("Content not compatible with " + facetType.name() + " facet type");
                }
                return new RelationFacet(label, count, (LabelCountListContent) rawContent);
            case ATTRIBUTE:
                if (!(LabelCountListContent.class.isAssignableFrom(rawContent.getClass()))) {
                    throw new RuntimeException("Content not compatible with " + facetType.name() + " facet type");
                }
                return new AttributeFacet(label, count, (LabelCountListContent) rawContent);
            default:
                throw new RuntimeException("Not supported facet type " + facetType);
        }

    }
}
