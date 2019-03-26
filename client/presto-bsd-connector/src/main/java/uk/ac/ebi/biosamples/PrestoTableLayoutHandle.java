package uk.ac.ebi.biosamples;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.prestosql.spi.connector.ColumnHandle;
import io.prestosql.spi.connector.ConnectorTableLayoutHandle;
import io.prestosql.spi.predicate.TupleDomain;

import java.util.Objects;

import static java.util.Objects.requireNonNull;

public class PrestoTableLayoutHandle implements ConnectorTableLayoutHandle {
    private final PrestoTableHandle table;
    private final TupleDomain<ColumnHandle> tupleDomain;

    @JsonCreator
    public PrestoTableLayoutHandle(@JsonProperty("table") PrestoTableHandle table,
                                   @JsonProperty("tupleDomain") TupleDomain<ColumnHandle> tupleDomain) {
        this.table = requireNonNull(table, "table is null");
        this.tupleDomain = requireNonNull(tupleDomain, "tuple is null");
    }

    @JsonProperty
    public PrestoTableHandle getTable() {
        return table;
    }

    @JsonProperty
    public TupleDomain<ColumnHandle> getTupleDomain() {
        return tupleDomain;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PrestoTableLayoutHandle that = (PrestoTableLayoutHandle) o;
        return Objects.equals(table, that.table) &&
                Objects.equals(tupleDomain, that.tupleDomain);
    }

    @Override
    public int hashCode() {
        return Objects.hash(table, tupleDomain);
    }

    @Override
    public String toString() {
        return table.toString();
    }
}
