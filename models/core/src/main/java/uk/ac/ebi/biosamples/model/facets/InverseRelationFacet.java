package uk.ac.ebi.biosamples.model.facets;

import org.springframework.hateoas.core.Relation;

import java.util.List;

@Relation(collectionRelation = "facets")
public class InverseRelationFacet extends StringListFacet {

    InverseRelationFacet(String label, long count, List attributeList) {
        super(label, count, attributeList);
    }

    @Override
    public FacetType getType() {
        return FacetType.INCOMING_RELATIONSHIP;
    }

    public static InverseRelationFacet build(String label, long count, List attributeList) {
        return new InverseRelationFacet(label, count, attributeList);
    }
}
