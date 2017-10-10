package uk.ac.ebi.biosamples.model.facets;

public class RelationFacet extends AbstractFacet {
    protected RelationFacet(String label, long count, LabelCountListContent content) {
        super(label, FacetType.OUTGOING_RELATIONSHIP, count, content);
    }
}
