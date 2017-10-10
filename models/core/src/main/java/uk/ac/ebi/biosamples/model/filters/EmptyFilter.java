package uk.ac.ebi.biosamples.model.filters;

import java.util.Optional;

public class EmptyFilter implements FilterContent{

    @Override
    public Object getContent() {
        return Optional.empty();
    }

    @Override
    public void merge(FilterContent otherContent) {
        // Don't need to do anything
    }
}
