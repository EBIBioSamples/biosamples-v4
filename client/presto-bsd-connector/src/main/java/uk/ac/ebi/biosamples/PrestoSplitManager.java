package uk.ac.ebi.biosamples;

import io.prestosql.spi.connector.*;
import uk.ac.ebi.biosamples.schema.PrestoSchemaMetadata;

import javax.inject.Inject;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;

public class PrestoSplitManager implements ConnectorSplitManager {
    private final String connectorId;
    private final PrestoSchemaMetadata schemaMetadata;

    @Inject
    public PrestoSplitManager(PrestoConnectorId connectorId, PrestoSchemaMetadata schemaMetadata) {
        this.connectorId = requireNonNull(connectorId, "connectorId is null").toString();
        this.schemaMetadata = requireNonNull(schemaMetadata, "schema metadata is null");
    }

    @Override
    public ConnectorSplitSource getSplits(ConnectorTransactionHandle handle, ConnectorSession session, ConnectorTableLayoutHandle layout, SplitSchedulingStrategy splitSchedulingStrategy) {
        PrestoTableLayoutHandle layoutHandle = (PrestoTableLayoutHandle) layout;
        PrestoTableHandle tableHandle = layoutHandle.getTable();
        PrestoTable table = schemaMetadata.getTable(tableHandle.getSchemaName(), tableHandle.getTableName());
        // this can happen if table is removed during a query
        checkState(table != null, "Table %s.%s no longer exists", tableHandle.getSchemaName(), tableHandle.getTableName());

        List<ConnectorSplit> splits = new ArrayList<>();
        for (URI uri : table.getSources()) {
            splits.add(new PrestoSplit(connectorId, tableHandle.getSchemaName(), tableHandle.getTableName(), uri, layoutHandle.getTupleDomain()));
        }
        Collections.shuffle(splits);

        return new FixedSplitSource(splits);
    }
}
