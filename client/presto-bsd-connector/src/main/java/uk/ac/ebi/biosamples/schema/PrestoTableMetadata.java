package uk.ac.ebi.biosamples.schema;

import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.prestosql.spi.type.VarcharType;
import uk.ac.ebi.biosamples.PrestoColumn;
import uk.ac.ebi.biosamples.PrestoConfig;
import uk.ac.ebi.biosamples.PrestoTable;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PrestoTableMetadata {
    private static final String DEFAULT_TABLE_NAME = "biosamples_table";

    public static Map<String, PrestoTable> getTableMetadata(PrestoConfig config) {
        ImmutableMap.Builder<String, PrestoTable> tableBuilder = ImmutableBiMap.builder();

        List<PrestoColumn> columns = new ArrayList<>();
        columns.add(new PrestoColumn("id", VarcharType.VARCHAR));
        columns.add(new PrestoColumn("name", VarcharType.VARCHAR));
        columns.add(new PrestoColumn("gender", VarcharType.VARCHAR));
        List<URI> sources = new ArrayList<>();
        sources.add(config.getBioSamplesClientUri());
        PrestoTable table = new PrestoTable(DEFAULT_TABLE_NAME, columns, sources);

        Map<String, PrestoTable> tables = new HashMap<>();
        tables.put(table.getName(), table);

        return tables;
    }

}
