package uk.ac.ebi.biosamples;

import com.google.common.base.Strings;
import io.airlift.slice.Slice;
import io.airlift.slice.Slices;
import io.prestosql.spi.connector.RecordCursor;
import io.prestosql.spi.type.Type;
import org.springframework.hateoas.Resource;
import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.Sample;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static io.prestosql.spi.type.BigintType.BIGINT;
import static io.prestosql.spi.type.BooleanType.BOOLEAN;
import static io.prestosql.spi.type.DoubleType.DOUBLE;
import static io.prestosql.spi.type.VarcharType.createUnboundedVarcharType;

public class PrestoRecordCursor implements RecordCursor {

    private final List<PrestoColumnHandle> columnHandles;
    private final int[] fieldToColumnIndex;
    private final long totalBytes;

    private List<String> fields;

    private final Iterator<Resource<Sample>> samplesIterator;

    public PrestoRecordCursor(List<PrestoColumnHandle> columnHandles, Iterable<Resource<Sample>> sampleResourceIterable) {
        this.columnHandles = columnHandles;

        fieldToColumnIndex = new int[columnHandles.size()];
        for (int i = 0; i < columnHandles.size(); i++) {
            PrestoColumnHandle columnHandle = columnHandles.get(i);
            fieldToColumnIndex[i] = columnHandle.getOrdinalPosition();
        }
        samplesIterator = sampleResourceIterable.iterator();
        totalBytes = 100;//todo
    }

    @Override
    public long getCompletedBytes() {
        return totalBytes;
    }

    @Override
    public long getReadTimeNanos() {
        return 0;
    }

    @Override
    public Type getType(int field) {
        checkArgument(field < columnHandles.size(), "Invalid field index");
        return columnHandles.get(field).getColumnType();
    }

    @Override
    public boolean advanceNextPosition() {
        if (!samplesIterator.hasNext()) {
            return false;
        }

        Sample sample = samplesIterator.next().getContent();
        String phenotype = null;
        String datasetId = null;
        String duoCodes = null;
        for (Attribute attribute : sample.getCharacteristics()) {
            if (attribute.getType().equalsIgnoreCase("phenotype")) {
                phenotype = attribute.getValue();
                continue;
            }
            if (attribute.getType().equalsIgnoreCase("ega dataset id")) {
                datasetId = attribute.getValue();
                continue;
            }
        }

//        if (sample.getDuoCodes() != null && !sample.getDuoCodes().isEmpty()) {
//
//        }

        fields = new ArrayList<>(Arrays.asList(sample.getAccession(), sample.getName(), phenotype, datasetId, duoCodes));

        return true;
    }

    private String getFieldValue(int field) {
        checkState(fields != null, "Cursor has not been advanced yet");

        int columnIndex = fieldToColumnIndex[field];
        return fields.get(columnIndex);
    }

    @Override
    public boolean getBoolean(int field) {
        checkFieldType(field, BOOLEAN);
        return Boolean.parseBoolean(getFieldValue(field));
    }

    @Override
    public long getLong(int field) {
        checkFieldType(field, BIGINT);
        return Long.parseLong(getFieldValue(field));
    }

    @Override
    public double getDouble(int field) {
        checkFieldType(field, DOUBLE);
        return Double.parseDouble(getFieldValue(field));
    }

    @Override
    public Slice getSlice(int field) {
        checkFieldType(field, createUnboundedVarcharType());
        return Slices.utf8Slice(getFieldValue(field));
    }

    @Override
    public Object getObject(int field) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isNull(int field) {
        checkArgument(field < columnHandles.size(), "Invalid field index");
        return Strings.isNullOrEmpty(getFieldValue(field));
    }

    private void checkFieldType(int field, Type expected) {
        Type actual = getType(field);
        checkArgument(actual.equals(expected), "Expected field %s to be type %s but is %s", field, expected, actual);
    }

    @Override
    public void close() {
    }
}
