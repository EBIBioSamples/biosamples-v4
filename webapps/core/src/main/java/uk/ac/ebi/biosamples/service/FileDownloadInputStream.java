package uk.ac.ebi.biosamples.service;

import org.apache.commons.io.IOUtils;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.filter.Filter;
import uk.ac.ebi.biosamples.solr.repo.CursorArrayList;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Queue;

public class FileDownloadInputStream extends InputStream {
    private static final int MAX_DOWNLOAD_SIZE = 100000;
    private static final int PAGE_SIZE = 1000;
    private static final String CURSOR_EOF = "EoF";

    private final SamplePageService samplePageService;
    private final String text;
    private final Collection<Filter> filters;
    private final Collection<String> domains;
    private final FileDownloadSerializer serializer;
    private final Queue<Sample> sampleQueue;
    private InputStream sampleStream;
    private String cursor;
    private int sampleCount;

    public FileDownloadInputStream(SamplePageService samplePageService, String text, Collection<Filter> filters,
                                   Collection<String> domains, FileDownloadSerializer serializer) {
        this.samplePageService = samplePageService;
        this.text = text;
        this.filters = filters;
        this.domains = domains;
        this.serializer = serializer;

        sampleQueue = new LinkedList<>();
        cursor = "*";
        sampleCount = 0;
    }

    @Override
    public int read() throws IOException {
        if (sampleStream == null) {
            sampleStream = generateStream(serializer.startOfFile());
        }

        int nextByte = sampleStream.read();
        if (nextByte == -1 && !CURSOR_EOF.equals(cursor)) {
            sampleStream = generateStream(serializer.delimiter());
            nextByte = sampleStream.read();
        }

        return nextByte;
    }

    private InputStream generateStream(String delimiter) throws IOException {
        InputStream inputStream;
        if (sampleQueue.isEmpty() && !CURSOR_EOF.equals(cursor)) {
            loadSamples();
        }

        if (!CURSOR_EOF.equals(cursor)) {
            Sample sample = sampleQueue.poll();
            sampleCount++;
            inputStream = toInputStream(delimiter, serializer.asString(sample));
            if (++sampleCount >= MAX_DOWNLOAD_SIZE) {
                cursor = CURSOR_EOF;
            }
        } else {
            inputStream = toInputStream(serializer.endOfFile(), "");
        }

        return inputStream;
    }

    private void loadSamples() {
        CursorArrayList<Sample> samplePage = samplePageService.getSamplesByText(text, filters, domains, cursor, PAGE_SIZE, null);
        if (!samplePage.isEmpty()) {
            sampleQueue.addAll(samplePage);
            cursor = samplePage.getNextCursorMark();
        } else {
            cursor = CURSOR_EOF; // mark end of samples
        }
    }

    private InputStream toInputStream(String delimiter, String sample) {
        return IOUtils.toInputStream(delimiter + sample, StandardCharsets.UTF_8);
    }
}
