package uk.ac.ebi.biosamples.model.facets;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.hateoas.core.Relation;
import uk.ac.ebi.biosamples.model.field.SampleFieldType;

@Relation(collectionRelation = "facets")
public class InverseRelationFacet extends Facet {


    @JsonCreator
    public InverseRelationFacet(@JsonProperty("label") String label, @JsonProperty("count") long count, @JsonProperty("content") LabelCountListContent content) {
        super(label, count, content);
    }

    @Override
    public SampleFieldType getFieldType() {
        return SampleFieldType.INVERSE_RELATION;
    }
}
