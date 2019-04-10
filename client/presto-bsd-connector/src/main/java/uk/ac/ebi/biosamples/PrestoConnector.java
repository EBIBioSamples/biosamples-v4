/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.ebi.biosamples;

import io.airlift.log.Logger;
import io.prestosql.spi.connector.*;
import io.prestosql.spi.transaction.IsolationLevel;

import javax.inject.Inject;

import static uk.ac.ebi.biosamples.PrestoTransactionHandle.INSTANCE;
import static java.util.Objects.requireNonNull;

public class PrestoConnector implements Connector {
    private static final Logger log = Logger.get(PrestoConnector.class);

    private final PrestoMetadata metadata;
    private final PrestoSplitManager splitManager;
    private final PrestoRecordSetProvider recordSetProvider;
    private final PrestoPageSourceProvider pageSourceProvider;

    @Inject
    public PrestoConnector(
            PrestoMetadata metadata,
            PrestoSplitManager splitManager,
            PrestoRecordSetProvider recordSetProvider,
            PrestoPageSourceProvider pageSourceProvider) {
        this.metadata = requireNonNull(metadata, "metadata is null");
        this.splitManager = requireNonNull(splitManager, "splitManager is null");
        this.recordSetProvider = requireNonNull(recordSetProvider, "recordSetProvider is null");
        this.pageSourceProvider = requireNonNull(pageSourceProvider, "pageSourceProvider is null");
    }

    @Override
    public ConnectorTransactionHandle beginTransaction(IsolationLevel isolationLevel, boolean readOnly) {
        return INSTANCE;
    }

    @Override
    public ConnectorMetadata getMetadata(ConnectorTransactionHandle transactionHandle) {
        return metadata;
    }

    @Override
    public ConnectorSplitManager getSplitManager() {
        return splitManager;
    }

    @Override
    public ConnectorRecordSetProvider getRecordSetProvider() {
        return recordSetProvider;
    }

    @Override
    public PrestoPageSourceProvider getPageSourceProvider() {
        return pageSourceProvider;
    }

    @Override
    public final void shutdown() {
        log.info("Shutting down connector");
    }
}
