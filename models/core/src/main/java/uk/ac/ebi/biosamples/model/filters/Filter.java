package uk.ac.ebi.biosamples.model.filters;

public interface Filter {

    public FilterType getKind();

    public String getLabel();

    public Object getContent();

    public String getSerialization();

    default FieldPresentFilter getFieldPresenceFilter() {
        if (this instanceof FieldPresentFilter) {
            return (FieldPresentFilter) this;
        }
        return new FieldPresentFilter(this.getKind(), this.getLabel());
    }

}
