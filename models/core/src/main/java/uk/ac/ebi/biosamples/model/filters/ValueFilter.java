package uk.ac.ebi.biosamples.model.filters;

public class ValueFilter implements FilterContent {

    private final String value;

    public ValueFilter(String value) {
        this.value =value;
    }

    public String getValue() {
        return this.value;
    }

    @Override
    public String getContent() {
        return value;
    }

}
