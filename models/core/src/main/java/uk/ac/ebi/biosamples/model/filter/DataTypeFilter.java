package uk.ac.ebi.biosamples.model.filter;

import uk.ac.ebi.biosamples.model.structured.DataType;

import java.util.Objects;
import java.util.Optional;

public class DataTypeFilter implements Filter {

    private DataType dataType;

    private DataTypeFilter(String name) {
        this.dataType = DataType.valueOf(name);
    }

    private DataTypeFilter(DataType dataType) {
        this.dataType = dataType;
    }

    @Override
    public FilterType getType() {
        return FilterType.DATA_TYPE_FILTER;
    }

    @Override
    public String getLabel() {
        return "structdatatype";
    }

    @Override
    public Optional<DataType> getContent() {
        return Optional.of(this.dataType);
    }

    @Override
    public String getSerialization() {
        return this.getType().getSerialization() + ":" + this.dataType.name();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof DataTypeFilter)) {
            return false;
        }
        DataTypeFilter other = (DataTypeFilter) obj;
        return Objects.equals(other.getContent().orElse(null), this.getContent().orElse(null));
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getContent().orElse(null));
    }

    public static class Builder implements Filter.Builder {

        private DataType dataType ;

        public Builder(DataType dataType) {
            this.dataType = dataType;
        }

        public Builder(String dataType) {
            this.dataType = DataType.valueOf(dataType);
        }

        @Override
        public Filter build() {
            return new DataTypeFilter(this.dataType);
        }

        @Override
        public Filter.Builder parseContent(String filterSerialized) {
            return new Builder(filterSerialized);
        }
    }
}
