package uk.ac.ebi.biosamples.model.facets;

import org.springframework.hateoas.core.Relation;

@Relation(collectionRelation = "facets")
public class InverseRelationFacet extends Facet {


    public InverseRelationFacet(String label, long count, LabelCountListContent content) {
        super(label, count, content);
    }

//    @Override
//    protected LabelCountListContent readContent(Object content) {
//        return (LabelCountListContent) content;
//    }
//
//    @Override
//    protected Class getCompatibleContent() {
//        return LabelCountListContent.class;
//    }

    @Override
    public FacetType getType() {
        return FacetType.INCOMING_RELATIONSHIP;
    }
}
