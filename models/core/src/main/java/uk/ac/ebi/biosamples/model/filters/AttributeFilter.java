package uk.ac.ebi.biosamples.model.filters;

import java.util.Objects;

public class AttributeFilter implements Filter{

    private String label;
    private String value;


    private AttributeFilter(String label, String value) {
        this.label = label;
        this.value = value;
    }

    @Override
    public FilterType getKind() {
        return FilterType.ATTRIBUTE_FILTER;
    }

    @Override
    public String getLabel() {
        return this.label;
    }

    @Override
    public String getContent() {
        return this.value;
    }

    @Override
    public String getSerialization() {
        return getKind().getSerialization() + ":" + this.label + ":" + this.value;
    }

    @Override
    public boolean equals(Object obj){
        if(obj == this) {
            return true;
        }
        if (!(obj instanceof AttributeFilter)) {
            return false;
        }
        AttributeFilter other = (AttributeFilter) obj;
        return Objects.equals(other.label, this.label) && Objects.equals(other.value, this.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.label, this.value);
    }

    public static class Builder implements FilterBuilder{
        private String value = "*";
        private String label;

        public Builder(String label) {
            this.label = label;
        }


        public Builder withValue(String value) {
            this.value = value;
            return this;
        }

        @Override
        public AttributeFilter build() {
            return new AttributeFilter(this.label, this.value);
        }

        @Override
        public Builder parseValue(String filterValue) {
            return this.withValue(filterValue);
        }

    }
}
