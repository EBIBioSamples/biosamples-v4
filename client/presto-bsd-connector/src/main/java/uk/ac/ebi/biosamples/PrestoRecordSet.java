package uk.ac.ebi.biosamples;

import com.google.common.collect.ImmutableList;
import io.airlift.slice.Slice;
import io.prestosql.spi.connector.ColumnHandle;
import io.prestosql.spi.connector.RecordCursor;
import io.prestosql.spi.connector.RecordSet;
import io.prestosql.spi.predicate.Domain;
import io.prestosql.spi.type.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.filter.Filter;
import uk.ac.ebi.biosamples.service.FilterBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.util.Objects.requireNonNull;

public class PrestoRecordSet implements RecordSet {
    private static final Logger LOG = LoggerFactory.getLogger(PrestoRecordSet.class);

    private final List<PrestoColumnHandle> columnHandles;
    private final List<Type> columnTypes;
    private final BioSamplesClient client;
    private final List<Filter> filters;

    public PrestoRecordSet(PrestoSplit split, List<PrestoColumnHandle> columnHandles, BioSamplesClient client) {
        requireNonNull(split, "split is null");
        this.client = requireNonNull(client, "client is null");

        this.columnHandles = requireNonNull(columnHandles, "column handles is null");
        ImmutableList.Builder<Type> types = ImmutableList.builder();
        for (PrestoColumnHandle column : columnHandles) {
            types.add(column.getColumnType());
        }
        this.columnTypes = types.build();

        filters = new ArrayList<>();
        if (split.getTupleDomain().getDomains().isPresent()) {
            for (Map.Entry<ColumnHandle, Domain> entry : split.getTupleDomain().getDomains().get().entrySet()) {
                PrestoColumnHandle column = (PrestoColumnHandle) entry.getKey();
                Domain domain = entry.getValue();
                String columnName = column.getColumnName();
                String value = ((Slice) domain.getValues().getSingleValue()).toStringUtf8();
                LOG.info("Query for {} = {}", columnName, value);

                switch (columnName) {
                    case "id":
                        filters.add(FilterBuilder.create().onAccession(value).build());
                        break;
                    case "name":
                        filters.add(FilterBuilder.create().onName(value).build());
                        break;
                    case "phenotype":
                        filters.add(FilterBuilder.create().onAttribute("phenotype").withValue(value).build());
                        break;
                    case "sex":
                        filters.add(FilterBuilder.create().onAttribute("sex").withValue(value).build());
                        break;
                    case "dataset":
                        filters.add(FilterBuilder.create().onAttribute("ega dataset id").withValue(value).build());
                        break;
                    case "duo_codes":
                        filters.add(FilterBuilder.create().onAttribute("data use conditions").withValue(value).build());
                        break;
                    default:
                        LOG.warn("No such a column as: {}", columnName);
                }
            }
        }
    }

    @Override
    public List<Type> getColumnTypes() {
        return columnTypes;
    }

    @Override
    public RecordCursor cursor() {
        return new PrestoRecordCursor(columnHandles, client.fetchSampleResourceAll(filters));
    }
}
