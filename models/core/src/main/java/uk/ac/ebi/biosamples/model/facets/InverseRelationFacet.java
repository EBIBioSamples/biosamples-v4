package uk.ac.ebi.biosamples.model.facets;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.hateoas.core.Relation;
import uk.ac.ebi.biosamples.model.filters.FieldPresentFilter;
import uk.ac.ebi.biosamples.model.filters.Filter;
import uk.ac.ebi.biosamples.model.filters.FilterType;

@Relation(collectionRelation = "facets")
public class InverseRelationFacet extends Facet {


    @JsonCreator
    public InverseRelationFacet(@JsonProperty("label") String label, @JsonProperty("count") long count, @JsonProperty("content") LabelCountListContent content) {
        super(label, count, content);
    }

    @Override
    public FacetType getType() {
        return FacetType.INCOMING_RELATIONSHIP;
    }

    @Override
    public Filter getFieldPresenceFilter() {
        return new FieldPresentFilter(FilterType.INVERSE_RELATION_FILTER, this.getLabel());
    }
}
