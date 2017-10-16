package uk.ac.ebi.biosamples.model.filters;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (!(obj instanceof ValueFilter)) {
            return false;
        }
        ValueFilter other = (ValueFilter) obj;
        return Objects.deepEquals(this.getContent(), other.getContent());
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getContent());
    }
}
