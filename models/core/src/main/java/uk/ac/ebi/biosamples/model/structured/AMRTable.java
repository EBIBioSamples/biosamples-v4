package uk.ac.ebi.biosamples.model.structured;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.net.URI;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

//@JsonDeserialize(builder = AMRTable.Builder.class)
public class AMRTable extends AbstractData implements Comparable<AMRTable> {

    private final URI schema;
    private final Set<AMREntry> amrEntries;

    public AMRTable(URI schema, Set<AMREntry> amrEntries) {
        this.schema = schema;
        this.amrEntries = amrEntries;
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
        return amrEntries;
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
                Objects.equals(amrEntries, amrTable.amrEntries);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getSchema(), amrEntries);
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

        @JsonCreator
        public Builder(URI schema) {
            this.schema = schema;
            this.amrEntries = new HashSet<>();
        }

        @JsonCreator
        public Builder(String schema) {
            this(URI.create(schema));
        }

        @JsonProperty
        public Builder withEntry(AMREntry entry) {
            this.amrEntries.add(entry);
            return this;
        }

        @JsonProperty
        public Builder withEntries(Collection<AMREntry> entries) {
            this.amrEntries.addAll(entries);
            return this;
        }

        public AMRTable build() {
            return new AMRTable(this.schema, this.amrEntries);
        }
    }



}
