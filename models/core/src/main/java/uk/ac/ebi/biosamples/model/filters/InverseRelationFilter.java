package uk.ac.ebi.biosamples.model.filters;

import java.util.Objects;
import java.util.Optional;

public class InverseRelationFilter implements Filter {

    private String label;
    private String value;

    private InverseRelationFilter(String label, String value) {
        this.label = label;
        this.value = value;
    }

    @Override
    public FilterType getType() {
        return FilterType.INVERSE_RELATION_FILTER;
    }

    @Override
    public String getLabel() {
        return this.label;
    }

    @Override
    public Optional<String> getContent() {
        return Optional.ofNullable(this.value);
    }


    @Override
    public String getSerialization() {
        StringBuilder serializationBuilder =
                new StringBuilder(this.getType().getSerialization())
                    .append(":")
                    .append(this.getLabel());
        this.getContent().ifPresent(content -> serializationBuilder.append(":").append(content));
        return serializationBuilder.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof InverseRelationFilter)) {
            return false;
        }
        InverseRelationFilter other = (InverseRelationFilter) obj;
        return Objects.equals(other.getLabel(), this.getLabel()) && Objects.equals(other.getContent().orElse(null), this.getContent().orElse(null));
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getLabel(), this.getContent().orElse(null));
    }

    public static class Builder implements Filter.Builder{
        private String value;
        private String label;

        public Builder(String label) {
            this.label = label;
        }

        public Builder withValue(String value) {
            this.value = value;
            return this;
        }

        @Override
        public InverseRelationFilter build() {
            return new InverseRelationFilter(this.label, this.value);
        }

        @Override
        public Builder parseContent(String filterValue) {
            return this.withValue(filterValue);
        }
    }
}
