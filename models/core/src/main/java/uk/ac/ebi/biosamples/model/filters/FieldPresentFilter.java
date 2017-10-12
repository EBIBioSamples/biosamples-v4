package uk.ac.ebi.biosamples.model.filters;

public class FieldPresentFilter extends Filter{

    public FieldPresentFilter(FilterType kind, String label) {
        super(kind, label, new EmptyFilter());
    }
}
