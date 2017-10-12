package uk.ac.ebi.biosamples.model.facets;

import org.springframework.hateoas.core.Relation;

@Relation(collectionRelation = "facets")
public class RelationFacet extends Facet {

    public RelationFacet(String label, long count, LabelCountListContent content) {
        super(label, count, content);
    }

//    @Override
//    protected FacetContent readContent(Object content) {
//        return (LabelCountListContent) content;
//    }
//
//    @Override
//    protected Class getCompatibleContent() {
//        return LabelCountListContent.class;
//    }

    @Override
    public FacetType getType() {
        return FacetType.OUTGOING_RELATIONSHIP;
    }
}
