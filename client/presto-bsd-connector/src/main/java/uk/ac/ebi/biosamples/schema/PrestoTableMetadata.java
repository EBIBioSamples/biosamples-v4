package uk.ac.ebi.biosamples.schema;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.prestosql.spi.type.VarcharType;
import uk.ac.ebi.biosamples.PrestoColumn;
import uk.ac.ebi.biosamples.PrestoConfig;
import uk.ac.ebi.biosamples.PrestoTable;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static java.util.Objects.requireNonNull;

public class PrestoTableMetadata {
    private static final String DEFAULT_TABLE_NAME = "biosamples_table";

    public static Map<String, PrestoTable> getTableMetadata(PrestoConfig config) {
        requireNonNull(config.getBioSamplesClientUri(), "BioSample client URI should not be null");

        List<PrestoColumn> columns = ImmutableList.of(
                new PrestoColumn("id", VarcharType.VARCHAR),
                new PrestoColumn("name", VarcharType.VARCHAR),
                new PrestoColumn("phenotype", VarcharType.VARCHAR),
                new PrestoColumn("sex", VarcharType.VARCHAR),
                new PrestoColumn("dataset", VarcharType.VARCHAR),
                new PrestoColumn("duo_codes", VarcharType.VARCHAR));
        List<URI> sources = Collections.singletonList(config.getBioSamplesClientUri());
        PrestoTable table = new PrestoTable(DEFAULT_TABLE_NAME, columns, sources);

        return ImmutableMap.of(table.getName(), table);
    }

}
