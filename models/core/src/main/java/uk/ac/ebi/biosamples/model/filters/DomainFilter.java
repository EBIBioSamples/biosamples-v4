package uk.ac.ebi.biosamples.model.filters;

import java.util.Optional;

public class DomainFilter implements Filter{

    private String domain;

    private DomainFilter(String domain) {
        this.domain = domain;
    }

    @Override
    public FilterType getKind() {
        return FilterType.DOMAIN_FILTER;
    }

    @Override
    public String getLabel() {
        return this.domain;
    }

    @Override
    public Optional<?> getContent() {
        return Optional.empty();
    }

    @Override
    public String getSerialization() {
        return this.getKind().getSerialization() + ":" + this.getLabel();
    }

    public static class Builder implements Filter.Builder {

        private String domain;

        public Builder(String domain) {
            this.domain = domain;
        }

        @Override
        public Filter build() {
            return new DomainFilter(this.domain);
        }

        @Override
        public Filter.Builder parseContent(String filterSerialized) {
            return null;
        }
    }
}
