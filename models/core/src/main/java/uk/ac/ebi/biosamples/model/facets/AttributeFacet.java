package uk.ac.ebi.biosamples.model.facets;

public class AttributeFacet extends AbstractFacet{

    protected AttributeFacet(String label, long count, LabelCountListContent content) {
        super(label, FacetType.ATTRIBUTE, count, content);
    }

}
