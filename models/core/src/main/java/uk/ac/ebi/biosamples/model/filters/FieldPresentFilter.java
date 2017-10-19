package uk.ac.ebi.biosamples.model.filters;

public class FieldPresentFilter implements Filter{

    FilterType type;
    String fieldLabel;

    public FieldPresentFilter(FilterType filterType, String fieldLabel) {
        this.type = filterType;
        this.fieldLabel = fieldLabel;
    }


    @Override
    public FilterType getKind() {
        return this.type;
    }

    @Override
    public String getLabel() {
        return this.fieldLabel;
    }

    @Override
    public Object getContent() {
        return null;
    }

    @Override
    public String getSerialization() {
        return String.format("%s:%s", this.getKind().getSerialization(), this.getLabel());
    }
}
