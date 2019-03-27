package uk.ac.ebi.biosamples.schema;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import uk.ac.ebi.biosamples.PrestoConfig;
import uk.ac.ebi.biosamples.PrestoTable;

import java.util.Map;
import java.util.Set;

import static java.util.Objects.requireNonNull;

public class PrestoSchemaMetadata {
    private static final String DEFAULT_SCHEMA_NAME = "biosamples_schema";

    private final Map<String, Map<String, PrestoTable>> schemas;

    public PrestoSchemaMetadata(PrestoConfig config) {
        requireNonNull(config, "config should not be null");
        schemas = ImmutableMap.of(DEFAULT_SCHEMA_NAME, PrestoTableMetadata.getTableMetadata(config));
    }

    public Set<String> getSchemaNames() {
        return schemas.keySet();
    }

    public Set<String> getTableNames(String schema) {
        requireNonNull(schema, "schema is null");
        Map<String, PrestoTable> tables = schemas.get(schema);
        if (tables == null) {
            return ImmutableSet.of();
        }
        return tables.keySet();
    }

    public PrestoTable getTable(String schema, String tableName) {
        requireNonNull(schema, "schema is null");
        requireNonNull(tableName, "tableName is null");
        Map<String, PrestoTable> tables = schemas.get(schema);
        if (tables == null) {
            return null;
        }
        return tables.get(tableName);
    }
}
