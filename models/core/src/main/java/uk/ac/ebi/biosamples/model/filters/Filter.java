package uk.ac.ebi.biosamples.model.filters;

public class Filter<T extends FilterContent> {

    private final FilterType kind;
    private final String label;

    private final T content;

    public Filter(FilterType kind, String label, T content) {
        this.kind = kind;
        this.label = label;
        this.content = content;
    }


    public FilterType getKind() {
        return kind;
    }

    public String getLabel() {
        return label;
    }

    public T getContent() {
        return content;
    }

    /**
     * Check if two filters are compatible in term of type and label but not content
     * This method is used to check
     * @param anotherFilter The filter to check agains
     * @return
     */
    public boolean isCompatible(Filter anotherFilter) {
        return this.label.equals(anotherFilter.getLabel()) && this.kind.equals(anotherFilter.getKind());
    }

}
