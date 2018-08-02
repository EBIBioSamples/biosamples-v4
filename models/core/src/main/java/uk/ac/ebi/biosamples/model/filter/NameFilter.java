package uk.ac.ebi.biosamples.model.filter;

import uk.ac.ebi.biosamples.model.facet.FacetType;

import java.util.Objects;
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
    public FacetType getAssociatedFacetType() {
        return FacetType.NO_TYPE;
    }

    @Override
    public String getSerialization() {
        return this.getType().getSerialization() + ":" + this.name;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof NameFilter)) {
            return false;
        }
        NameFilter other = (NameFilter) obj;
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
            return new NameFilter(this.domain);
        }

        @Override
        public Filter.Builder parseContent(String filterSerialized) {
            return new Builder(filterSerialized);
        }
    }
}
