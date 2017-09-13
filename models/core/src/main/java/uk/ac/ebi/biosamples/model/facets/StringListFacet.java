package uk.ac.ebi.biosamples.model.facets;

import java.util.Collections;
import java.util.List;

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

