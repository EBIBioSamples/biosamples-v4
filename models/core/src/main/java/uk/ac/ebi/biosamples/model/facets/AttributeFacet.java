package uk.ac.ebi.biosamples.model.facets;

import org.springframework.hateoas.core.Relation;

import java.util.List;

@Relation(collectionRelation = "facets")
public class AttributeFacet extends StringListFacet {


    AttributeFacet(String label, long count, List attributeList) {
        super(label, count, attributeList);
    }

    @Override
    public FacetType getType() {
        return FacetType.ATTRIBUTE;
    }


}
