package uk.ac.ebi.biosamples.model.structured;

import java.net.URI;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class AMRTable extends AbstractData implements Comparable<AMRTable> {

    private final URI schema;
    private final Set<AMREntry> data;

    public AMRTable(URI schema, Set<AMREntry> amrEntries) {
        this.schema = schema;
        this.data = amrEntries;
    }

    @Override
    public DataType getDataType() {
        return DataType.AMR;
    }

    @Override
    public URI getSchema() {
        return schema;
    }

    @Override
    public Set<AMREntry> getStructuredData() {
        return data;
    }

    @Override
    public int compareTo(AMRTable other) {
        if (other == null) {
            return 1;
        }

        return nullSafeStringComparison(this.schema.toString(), other.schema.toString());
        //TODO does it make sense to compare amr data?
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AMRTable)) return false;
        AMRTable amrTable = (AMRTable) o;
        return Objects.equals(getSchema(), amrTable.getSchema()) &&
                Objects.equals(data, amrTable.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getSchema(), data);
    }

    private int nullSafeStringComparison(String one, String two) {

        if (one == null && two != null) {
            return -1;
        }
        if (one != null && two == null) {
            return 1;
        }
        if (one != null && !one.equals(two)) {
            return one.compareTo(two);
        }

        return 0;
    }

    public static class Builder {
        private URI schema;
        private Set<AMREntry> amrEntries;

        public Builder(URI schema) {
            this.schema = schema;
            this.amrEntries = new HashSet<>();
        }

        public Builder(String schema) {
            this(URI.create(schema));
        }

        public Builder addEntry(AMREntry entry) {
            this.amrEntries.add(entry);
            return this;
        }

        public AMRTable build() {
            return new AMRTable(this.schema, this.amrEntries);
        }
    }



}
