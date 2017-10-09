package uk.ac.ebi.biosamples.model.filters;

import java.util.Optional;

public class EmptyFilter implements FilterContent{

    @Override
    public Object getContent() {
        return Optional.empty();
    }
}
