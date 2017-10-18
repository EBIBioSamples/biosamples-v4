package uk.ac.ebi.biosamples.model.filters;

public class ValueFilterBuilder{

    private FilterType type;
    private String label;
    private FilterContent content;

    public ValueFilterBuilder(FilterType type, String label) {
        this.type = type;
        this.label = label;
        this.content = new EmptyFilter();
    }

    public ValueFilterBuilder withValue(String value) {
        this.content = new ValueFilter(value);
        return this;
    }

    public Filter build() {
        return new Filter(this.type, this.label, this.content);
    }



}
