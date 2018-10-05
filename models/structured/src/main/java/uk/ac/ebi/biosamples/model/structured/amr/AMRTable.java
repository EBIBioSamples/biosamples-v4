package uk.ac.ebi.biosamples.model.structured.amr;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import uk.ac.ebi.biosamples.model.structured.AbstractData;
import uk.ac.ebi.biosamples.model.structured.DataType;

import java.net.URI;
import java.util.*;

//@JsonDeserialize(builder = AMRTable.Builder.class)
public class AMRTable extends AbstractData implements Comparable<AbstractData> {

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
    public int compareTo(AbstractData other) {

        if (other == null) {
            return 1;
        }

        if ( !(other instanceof AMRTable) ) {
            return 1;
        }

        AMRTable otherAmrTable = (AMRTable) other;
        Set<AMREntry> otherTableAMREntries = otherAmrTable.getStructuredData();
        for (AMREntry entry: this.getStructuredData()) {
            Optional<AMREntry> otherEntry = otherTableAMREntries.parallelStream()
                    .filter(e -> e.equals(entry)).findFirst();
            if (! otherEntry.isPresent()) {
                return 1;
            } else {
                int comparison = entry.compareTo(otherEntry.get());
                if (0 != comparison) {
                    return comparison;
                }
            }
        }


        return nullSafeStringComparison(this.schema.toString(), otherAmrTable.schema.toString());
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
        public Builder addEntry(AMREntry entry) {
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
