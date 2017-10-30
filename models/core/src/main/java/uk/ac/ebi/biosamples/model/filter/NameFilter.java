package uk.ac.ebi.biosamples.model.filter;

import java.util.Optional;

public class NameFilter implements Filter {

    private String name;

    private NameFilter(String name) {
        this.name = name;
    }

    @Override
    public FilterType getType() {
        return FilterType.NAME_FILTER;
    }

    @Override
    public String getLabel() {
        return "name";
    }

    @Override
    public Optional<String> getContent() {
        return Optional.of(this.name);
    }

    @Override
    public String getSerialization() {
        return this.getType().getSerialization() + ":" + this.name;
    }

    public static class Builder implements Filter.Builder {

        private String domain;

        public Builder(String domain) {
            this.domain = domain;
        }

        @Override
        public Filter build() {
            return new NameFilter(this.domain);
        }

        @Override
        public Filter.Builder parseContent(String filterSerialized) {
            return new Builder(filterSerialized);
        }
    }
}
