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

import com.google.common.collect.ImmutableList;
import io.prestosql.spi.connector.RecordCursor;
import io.prestosql.spi.connector.RecordSet;
import io.prestosql.spi.type.Type;
import uk.ac.ebi.biosamples.client.BioSamplesClient;

import java.util.List;

import static java.util.Objects.requireNonNull;

public class PrestoRecordSet implements RecordSet {
    private final List<PrestoColumnHandle> columnHandles;
    private final List<Type> columnTypes;
    private final BioSamplesClient client;

    public PrestoRecordSet(PrestoSplit split, List<PrestoColumnHandle> columnHandles, BioSamplesClient client) {
        requireNonNull(split, "split is null");
        this.client = requireNonNull(client, "client is null");

        this.columnHandles = requireNonNull(columnHandles, "column handles is null");
        ImmutableList.Builder<Type> types = ImmutableList.builder();
        for (PrestoColumnHandle column : columnHandles) {
            types.add(column.getColumnType());
        }
        this.columnTypes = types.build();
    }

    @Override
    public List<Type> getColumnTypes() {
        return columnTypes;
    }

    @Override
    public RecordCursor cursor() {
        System.out.println("This is the client cursor start point");
        return new PrestoRecordCursor(columnHandles, client.fetchSampleResourceAll());
    }
}
