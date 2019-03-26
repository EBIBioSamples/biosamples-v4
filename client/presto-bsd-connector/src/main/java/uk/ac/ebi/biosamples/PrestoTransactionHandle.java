package uk.ac.ebi.biosamples;

import io.prestosql.spi.connector.ConnectorTransactionHandle;

public enum PrestoTransactionHandle implements ConnectorTransactionHandle {
    INSTANCE
}
