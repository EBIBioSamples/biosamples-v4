package uk.ac.ebi.biosamples.model.facets;

import org.springframework.hateoas.core.Relation;
import uk.ac.ebi.biosamples.model.filters.FieldPresentFilter;
import uk.ac.ebi.biosamples.model.filters.Filter;
import uk.ac.ebi.biosamples.model.filters.FilterType;

@Relation(collectionRelation = "facets")
public class RelationFacet extends Facet {

    public RelationFacet(String label, long count, LabelCountListContent content) {
        super(label, count, content);
    }

    @Override
    public FacetType getType() {
        return FacetType.OUTGOING_RELATIONSHIP;
    }

    @Override
    public Filter getFieldPresenceFilter() {
        return new FieldPresentFilter(FilterType.RELATION_FILER, this.getLabel());
    }
}
