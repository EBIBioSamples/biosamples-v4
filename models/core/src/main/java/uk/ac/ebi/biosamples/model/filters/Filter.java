package uk.ac.ebi.biosamples.model.filters;

import java.util.Optional;

public interface Filter {

    public FilterType getType();

    public String getLabel();

    public Optional<?> getContent();

    public String getSerialization();

    public interface Builder {
        public Filter build();

        public Filter.Builder parseContent(String filterSerialized);

    }
}
