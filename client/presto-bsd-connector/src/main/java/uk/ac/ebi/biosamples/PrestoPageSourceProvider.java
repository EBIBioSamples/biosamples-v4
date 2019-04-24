package uk.ac.ebi.biosamples;

import io.prestosql.spi.connector.*;

import java.util.List;

import static java.util.Objects.requireNonNull;

public class PrestoPageSourceProvider implements ConnectorPageSourceProvider {

    private final int pageSize;
    private final ConnectorRecordSetProvider recordSetProvider;

    public PrestoPageSourceProvider(PrestoRecordSetProvider recordSetProvider, int pageSize) {
        this.recordSetProvider = requireNonNull(recordSetProvider, "recordSetProvider is null");
        this.pageSize = pageSize;
    }

    @Override
    public ConnectorPageSource createPageSource(
            ConnectorTransactionHandle transactionHandle,
            ConnectorSession session,
            ConnectorSplit split,
            List<ColumnHandle> columns) {
        return new PrestoPageSource(
                recordSetProvider.getRecordSet(transactionHandle, session, split, columns),
                pageSize);
    }
}
