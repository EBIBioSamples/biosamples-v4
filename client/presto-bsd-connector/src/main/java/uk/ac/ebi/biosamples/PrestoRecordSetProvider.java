package uk.ac.ebi.biosamples;

import com.google.common.collect.ImmutableList;
import io.prestosql.spi.connector.*;
import uk.ac.ebi.biosamples.client.BioSamplesClient;

import javax.inject.Inject;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

public class PrestoRecordSetProvider implements ConnectorRecordSetProvider {
    private final String connectorId;
    private BioSamplesClient client;
    private PrestoMetadata metadata;

    @Inject
    public PrestoRecordSetProvider(PrestoConnectorId connectorId, BioSamplesClient client, PrestoMetadata metadata) {
        this.connectorId = requireNonNull(connectorId, "connectorId is null").toString();
        this.client = requireNonNull(client, "client is null");
        this.metadata = requireNonNull(metadata, "metadata is null");
    }

    @Override
    public RecordSet getRecordSet(ConnectorTransactionHandle transactionHandle,
                                  ConnectorSession session,
                                  ConnectorSplit split,
                                  List<? extends ColumnHandle> columns) {
        requireNonNull(split, "partitionChunk is null");
        PrestoSplit exampleSplit = (PrestoSplit) split;
        checkArgument(exampleSplit.getConnectorId().equals(connectorId), "split is not for this connector");

        ImmutableList.Builder<PrestoColumnHandle> handles = ImmutableList.builder();
        for (ColumnHandle handle : columns) {
            handles.add((PrestoColumnHandle) handle);
        }

        return new PrestoRecordSet(exampleSplit, handles.build(), client);
    }
}
