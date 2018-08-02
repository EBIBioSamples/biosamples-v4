package uk.ac.ebi.biosamples.model.filter;

import uk.ac.ebi.biosamples.model.facet.FacetType;

import java.util.Objects;
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

    @Override
    public FacetType getAssociatedFacetType() {
        return FacetType.NO_TYPE;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof DomainFilter)) {
            return false;
        }
        DomainFilter other = (DomainFilter) obj;
        return Objects.equals(other.getContent().orElse(null), this.getContent().orElse(null));
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getContent().orElse(null));
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
