package uk.ac.ebi.biosamples;

import io.airlift.slice.Slice;
import io.prestosql.spi.Page;
import io.prestosql.spi.PageBuilder;
import io.prestosql.spi.block.BlockBuilder;
import io.prestosql.spi.connector.ConnectorPageSource;
import io.prestosql.spi.connector.RecordCursor;
import io.prestosql.spi.connector.RecordPageSource;
import io.prestosql.spi.connector.RecordSet;
import io.prestosql.spi.type.Type;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;

/**
 * This is basically a copy of {@link RecordPageSource} that allows configurable page size.
 */
public class PrestoPageSource implements ConnectorPageSource {
    private final int rowsPerPage;
    private final RecordCursor cursor;
    private final List<Type> types;
    private final PageBuilder pageBuilder;
    private boolean closed;

    public PrestoPageSource(RecordSet recordSet, int rowsPerPage) {
        this(requireNonNull(recordSet, "recordSet is null").getColumnTypes(),
                recordSet.cursor(), rowsPerPage);
    }

    public PrestoPageSource(List<Type> types, RecordCursor cursor, int rowsPerPage) {
        this.cursor = requireNonNull(cursor, "cursor is null");
        this.types = unmodifiableList(new ArrayList<>(requireNonNull(types, "types is null")));
        this.pageBuilder = PageBuilder.withMaxPageSize(16_000, types);
        this.rowsPerPage = rowsPerPage;
    }

    public RecordCursor getCursor() {
        return cursor;
    }

    @Override
    public long getCompletedBytes() {
        return cursor.getCompletedBytes();
    }

    @Override
    public long getReadTimeNanos() {
        return cursor.getReadTimeNanos();
    }

    @Override
    public long getSystemMemoryUsage() {
        return cursor.getSystemMemoryUsage() + pageBuilder.getSizeInBytes();
    }

    @Override
    public void close() {
        closed = true;
        cursor.close();
    }

    @Override
    public boolean isFinished() {
        return closed && pageBuilder.isEmpty();
    }

    @Override
    public Page getNextPage() {
        if (!closed) {
            int i;
            for (i = 0; i < rowsPerPage; i++) {
                if (pageBuilder.isFull()) {
                    break;
                }

                if (!cursor.advanceNextPosition()) {
                    closed = true;
                    break;
                }

                pageBuilder.declarePosition();
                for (int column = 0; column < types.size(); column++) {
                    BlockBuilder output = pageBuilder.getBlockBuilder(column);
                    if (cursor.isNull(column)) {
                        output.appendNull();
                    } else {
                        Type type = types.get(column);
                        Class<?> javaType = type.getJavaType();
                        if (javaType == boolean.class) {
                            type.writeBoolean(output, cursor.getBoolean(column));
                        } else if (javaType == long.class) {
                            type.writeLong(output, cursor.getLong(column));
                        } else if (javaType == double.class) {
                            type.writeDouble(output, cursor.getDouble(column));
                        } else if (javaType == Slice.class) {
                            Slice slice = cursor.getSlice(column);
                            type.writeSlice(output, slice, 0, slice.length());
                        } else {
                            type.writeObject(output, cursor.getObject(column));
                        }
                    }
                }
            }
        }

        // only return a page if the buffer is full or we are finishing
        if (pageBuilder.isEmpty() || (!closed && !pageBuilder.isFull())) {
            return null;
        }

        Page page = pageBuilder.build();
        pageBuilder.reset();

        return page;
    }
}
