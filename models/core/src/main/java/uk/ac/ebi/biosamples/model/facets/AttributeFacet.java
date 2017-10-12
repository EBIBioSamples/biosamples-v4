package uk.ac.ebi.biosamples.model.facets;

import org.springframework.hateoas.core.Relation;

@Relation(collectionRelation = "facets")
public class AttributeFacet extends Facet {

    public AttributeFacet(String label, long count, LabelCountListContent content) {
        super(label, count, content);
    }

    @Override
    public FacetType getType(){
        return FacetType.ATTRIBUTE;
    }


}
