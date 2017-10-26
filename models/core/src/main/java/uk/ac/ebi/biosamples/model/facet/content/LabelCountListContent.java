package uk.ac.ebi.biosamples.model.facet.content;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.AbstractList;
import java.util.List;

public class LabelCountListContent extends AbstractList<LabelCountEntry> implements FacetContent{

    private List<LabelCountEntry> labelCountEntryList;

    @JsonCreator
    public LabelCountListContent(List<LabelCountEntry> labelCountEntryList) {
        this.labelCountEntryList = labelCountEntryList;
    }

    @Override
    public LabelCountEntry get(int index) {
        return labelCountEntryList.get(index);
    }

    @Override
    public int size() {
        return labelCountEntryList.size();
    }
}
