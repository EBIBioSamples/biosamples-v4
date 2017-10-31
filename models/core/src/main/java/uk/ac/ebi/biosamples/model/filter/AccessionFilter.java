package uk.ac.ebi.biosamples.model.filter;

import java.util.Objects;
import java.util.Optional;

public class AccessionFilter implements Filter {

    private String accessionPattern;

    private AccessionFilter(String accessionPattern) {
        this.accessionPattern = accessionPattern;
    }

    @Override
    public FilterType getType() {
        return FilterType.ACCESSION_FILTER;
    }

    @Override
    public String getLabel() {
        return "id";
    }

    @Override
    public Optional<String> getContent() {
        return Optional.of(this.accessionPattern);
    }

    @Override
    public String getSerialization() {
        return this.getType().getSerialization() + ":" + this.accessionPattern;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof AccessionFilter)) {
            return false;
        }
        AccessionFilter other = (AccessionFilter) obj;
        return Objects.equals(other.getContent().orElse(null), this.getContent().orElse(null));
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getContent().orElse(null));
    }

    public static class Builder implements Filter.Builder {

        private String pattern;

        public Builder(String pattern) {
            this.pattern = pattern;
        }

        @Override
        public Filter build() {
            return new AccessionFilter(this.pattern);
        }

        @Override
        public Filter.Builder parseContent(String filterSerialized) {
            return new Builder(filterSerialized);
        }
    }

}

