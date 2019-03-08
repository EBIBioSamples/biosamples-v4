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

import com.google.common.collect.ImmutableSet;
import io.prestosql.spi.type.VarcharType;

import javax.inject.Inject;
import java.net.URI;
import java.util.*;

import static java.util.Objects.requireNonNull;

public class PrestoClient {
    /**
     * SchemaName -> (TableName -> TableMetadata)
     */
    private final Map<String, Map<String, PrestoTable>> schemas;

    @Inject
    public PrestoClient(PrestoConfig config) {
        requireNonNull(config, "config is null");

        List<PrestoColumn> columns = new ArrayList<>();
        columns.add(new PrestoColumn("id", VarcharType.VARCHAR));
        columns.add(new PrestoColumn("name", VarcharType.VARCHAR));
        List<URI> sources = new ArrayList<>();
        try {
//            sources.add(new URI("https://wwwdev.ebi.ac.uk/biosamples"));
            sources.add(new URI("http://localhost:8090"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        PrestoTable table = new PrestoTable("numbers1", columns, sources);
        Map<String, PrestoTable> tables = new HashMap();
        tables.put("numbers1", table);


        schemas = new HashMap<>();
        schemas.put("example1", tables);

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
