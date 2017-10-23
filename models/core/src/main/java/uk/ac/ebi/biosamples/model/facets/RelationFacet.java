package uk.ac.ebi.biosamples.model.facets;

import org.springframework.hateoas.core.Relation;
import uk.ac.ebi.biosamples.model.facets.content.LabelCountListContent;
import uk.ac.ebi.biosamples.model.field.SampleFieldType;

@Relation(collectionRelation = "facets")
public class RelationFacet extends Facet {

    public RelationFacet(String label, long count, LabelCountListContent content) {
        super(label, count, content);
    }


    @Override
    public SampleFieldType getFieldType() {
        return SampleFieldType.RELATION;
    }
}
