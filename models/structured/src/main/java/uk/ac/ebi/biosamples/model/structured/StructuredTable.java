package uk.ac.ebi.biosamples.model.structured;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import uk.ac.ebi.biosamples.utils.StringUtils;

import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

public class StructuredTable<T extends StructuredEntry> extends AbstractData implements Comparable<AbstractData> {
    private final URI schema;
    private final String domain;
    private final DataType type;
    private final Set<T> entries;

    public StructuredTable(URI schema, String domain, DataType type, Set<T> entries) {
        this.schema = schema;
        this.domain = domain;
        this.type = type;
        this.entries = entries;
    }

    @Override
    public URI getSchema() {
        return schema;
    }

    @Override
    public String getDomain() {
        return domain;
    }

    @Override
    public DataType getDataType() {
        return type;
    }

    @Override
    public Set<T> getStructuredData() {
        return entries;
    }

    @Override
    public List<String> getHeaders() {
        return type.getHeaders();
    }

    @Override
    public List<Map<String, StructuredCell>> getDataAsMap() {
        return entries.stream().map(StructuredEntry::getDataAsMap).collect(Collectors.toUnmodifiableList());
    }

    @Override
    public int compareTo(AbstractData other) {
        if (other == null) {
            return 1;
        }

        if (!(other instanceof StructuredTable)) {
            return 1;
        }

        return StringUtils.nullSafeStringComparison(this.getDomain(), other.getDomain());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o instanceof StructuredTable) {
            StructuredTable<T> other = (StructuredTable<T>) o;
            return this.getDataType().equals(other.getDataType()) &&
                    this.getSchema().equals(other.getSchema()) && this.getDomain().equals(other.getDomain());
        }

        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(schema, entries, domain);
    }

    public static class Builder<T extends StructuredEntry> {
        private URI schema;
        private Set<T> entries;
        private String domain;
        private DataType type;

        @JsonCreator
        public Builder(URI schema, String domain, DataType type) {
            this.schema = schema;
            this.domain = domain;
            this.type = type;
            this.entries = new HashSet<>();
        }

        @JsonCreator
        public Builder(String schema, String domain, DataType type) {
            this(URI.create(schema), domain, type);
        }

        @JsonProperty
        public Builder<T> addEntry(T entry) {
            this.entries.add(entry);
            return this;
        }

        @JsonProperty
        public Builder<T> withEntries(Collection<T> entries) {
            this.entries.addAll(entries);
            return this;
        }

        public StructuredTable<T> build() {
            return new StructuredTable<>(this.schema, this.domain, this.type, this.entries);
        }
    }
}
