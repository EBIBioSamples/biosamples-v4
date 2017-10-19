package uk.ac.ebi.biosamples.model.facets;

import org.springframework.hateoas.core.Relation;

@Relation(collectionRelation = "facets")
public class RelationFacet extends Facet {

    public RelationFacet(String label, long count, LabelCountListContent content) {
        super(label, count, content);
    }

    @Override
    public FacetType getType() {
        return FacetType.OUTGOING_RELATIONSHIP;
    }

}
