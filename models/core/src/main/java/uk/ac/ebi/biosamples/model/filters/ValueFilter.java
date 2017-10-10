package uk.ac.ebi.biosamples.model.filters;

import java.util.ArrayList;
import java.util.List;

public class ValueFilter implements FilterContent {

    private final List<String> value;

    public ValueFilter() {
        this.value = new ArrayList<>();
    }

    public ValueFilter(List<String> value) {
        this.value =value;
    }

    @Override
    public List<String> getContent() {
        return value;
    }

    @Override
    public void merge(FilterContent otherContent) {
        if (otherContent instanceof ValueFilter) {
            ValueFilter decodedContent = (ValueFilter) otherContent;
            this.value.addAll(decodedContent.getContent());
        }
    }

}
