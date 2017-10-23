package uk.ac.ebi.biosamples.model.facets;

import uk.ac.ebi.biosamples.model.facets.content.LabelCountListContent;

public class FacetFactory {

    public static Facet build( FacetType facetType, String label, long count, FacetContent rawContent) {

        switch(facetType) {
            case INVERSE_RELATION_FACET:
                if (!(LabelCountListContent.class.isAssignableFrom(rawContent.getClass()))) {
                    throw new RuntimeException("Content not compatible with " + facetType.name() + " facet type");
                }
                return new InverseRelationFacet(label, count, (LabelCountListContent) rawContent);
            case RELATION_FACET:
                if (!(LabelCountListContent.class.isAssignableFrom(rawContent.getClass()))) {
                    throw new RuntimeException("Content not compatible with " + facetType.name() + " facet type");
                }
                return new RelationFacet(label, count, (LabelCountListContent) rawContent);
            case ATTRIBUTE_FACET:
                if (!(LabelCountListContent.class.isAssignableFrom(rawContent.getClass()))) {
                    throw new RuntimeException("Content not compatible with " + facetType.name() + " facet type");
                }
                return new AttributeFacet(label, count, (LabelCountListContent) rawContent);
            default:
                throw new RuntimeException("Not supported facet type " + facetType);
        }

    }
}
