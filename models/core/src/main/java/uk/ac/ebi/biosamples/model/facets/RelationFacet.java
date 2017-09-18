package uk.ac.ebi.biosamples.model.facets;

import org.springframework.hateoas.core.Relation;

import java.util.List;

@Relation(collectionRelation = "facets")
public class RelationFacet extends StringListFacet {


    RelationFacet(String label, long count, List attributeList) {
        super(label, count, attributeList);
    }

    @Override
    public FacetType getType() {
        return FacetType.OUTGOING_RELATIONSHIP;
    }

    public static RelationFacet build(String label, long count, List attributeList) {
        return new RelationFacet(label, count, attributeList);
    }

}
