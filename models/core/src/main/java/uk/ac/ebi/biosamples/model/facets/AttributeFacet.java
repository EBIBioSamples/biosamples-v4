package uk.ac.ebi.biosamples.model.facets;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.hateoas.core.Relation;
import uk.ac.ebi.biosamples.model.facets.content.LabelCountListContent;
import uk.ac.ebi.biosamples.model.field.SampleFieldType;

@Relation(collectionRelation = "facets")
public class AttributeFacet extends Facet {

    @JsonCreator
    public AttributeFacet(@JsonProperty("label") String label, @JsonProperty("count") long count, @JsonProperty("content") LabelCountListContent content) {
        super(label, count, content);
    }


    @Override
    public SampleFieldType getFieldType() {
        return SampleFieldType.ATTRIBUTE;
    }
}
