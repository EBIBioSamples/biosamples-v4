package uk.ac.ebi.biosamples.model.filters;

import java.util.Optional;

public class DomainFilter implements Filter{

    private String domain;

    private DomainFilter(String domain) {
        this.domain = domain;
    }

    @Override
    public FilterType getType() {
        return FilterType.DOMAIN_FILTER;
    }

    @Override
    public String getLabel() {
        return "domain";
    }

    @Override
    public Optional<String> getContent() {
        return Optional.of(this.domain);
    }

    @Override
    public String getSerialization() {
        return this.getType().getSerialization() + ":" + this.domain;
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
            return new Builder(filterSerialized);
        }
    }
}
