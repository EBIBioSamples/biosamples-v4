package uk.ac.ebi.biosamples.model.facets;

public class InverseRelationFacet extends AbstractFacet{
    protected InverseRelationFacet(String label, FacetType type, long count, LabelCountListContent content) {
        super(label, FacetType.INCOMING_RELATIONSHIP, count, content);
    }
}
