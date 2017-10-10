package uk.ac.ebi.biosamples.model.facets;

import java.util.List;
import static java.util.Map.Entry;

public class LabelCountListContent implements FacetContent {

    private List<Entry<String, Long>> labelCountEntryList;

    public LabelCountListContent(List<Entry<String, Long>> labelCountEntryList) {
        this.labelCountEntryList = labelCountEntryList;
    }

    @Override
    public List<Entry<String, Long>> getContent() {
        return labelCountEntryList;
    }

}
