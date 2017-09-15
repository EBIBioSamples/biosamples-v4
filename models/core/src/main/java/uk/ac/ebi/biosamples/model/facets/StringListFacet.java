package uk.ac.ebi.biosamples.model.facets;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.Collections;
import java.util.List;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY)
@JsonSubTypes({
        @JsonSubTypes.Type(value = AttributeFacet.class, name = "Attribute"),
        @JsonSubTypes.Type(value = InverseRelationFacet.class, name = "Inverse relation"),
        @JsonSubTypes.Type(value = RelationFacet.class, name= "Relation")
})
public abstract class StringListFacet extends Facet {

    private List attributeList;

    StringListFacet(String label, long count, List attributeList) {
        super(label, count);
        this.attributeList = attributeList;
    }

    @Override
    public List getContent() {
        return Collections.unmodifiableList(this.attributeList);
    }

}

