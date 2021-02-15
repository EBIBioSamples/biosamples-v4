package uk.ac.ebi.biosamples.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
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
    private static final int PAGE_SIZE = 1000;
    private final SamplePageService samplePageService;
    private final String text;
    private final Collection<Filter> filters;
    private final Collection<String> domains;
    private final FileDownloadSerializer serializer;
    private final ObjectMapper objectMapper;
    private final Queue<Sample> sampleQueue;
    private InputStream sampleStream;
    private String cursor;
    private int sampleCount;

    private static final int MAX_DOWNLOAD_SIZE = 100000;
    private static final String START_OF_CONTENT = "[";
    private static final String END_OF_CONTENT = "]";
    private static final String DELIMITER = "," + System.lineSeparator();
    private static final String CURSOR_EOF = "EoF";

    public FileDownloadInputStream(SamplePageService samplePageService, String text, Collection<Filter> filters, Collection<String> domains, FileDownloadSerializer serializer) {
        this.samplePageService = samplePageService;
        this.text = text;
        this.filters = filters;
        this.domains = domains;
        this.serializer = serializer;
//        objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        objectMapper = new XmlMapper().enable(SerializationFeature.INDENT_OUTPUT);
        sampleQueue = new LinkedList<>();
        cursor = "*";
        sampleCount = 0;
    }

    @Override
    public int read() throws IOException {
        if (sampleStream == null) {
            sampleStream = generateStream(START_OF_CONTENT);
        }

        int nextByte = sampleStream.read();
        if (nextByte == -1 && cursor != null) {
            sampleStream = generateStream(DELIMITER);
            nextByte = sampleStream.read();
        }

        return nextByte;
    }

    private InputStream generateStream(String delimiter) throws IOException {
        InputStream inputStream;
        if (sampleQueue.isEmpty() && cursor != null) {
            loadSamples();
        }

        if (cursor != null) {
            Sample sample = sampleQueue.poll();
            sampleCount++;
            inputStream = toInputStream(delimiter, objectMapper.writeValueAsString(sample));
            if (++sampleCount >= MAX_DOWNLOAD_SIZE) {
                cursor = null;
            }
        } else {
            inputStream = toInputStream(END_OF_CONTENT, "");
        }

        return inputStream;
    }

    private void loadSamples() {
        CursorArrayList<Sample> samplePage = samplePageService.getSamplesByText(text, filters, domains, cursor, PAGE_SIZE, null);
        if (!samplePage.isEmpty()) {
            sampleQueue.addAll(samplePage);
            cursor = samplePage.getNextCursorMark();
        } else {
            cursor = null; // mark end of samples
        }
    }

    private InputStream toInputStream(String delimiter, String sample) {
        return IOUtils.toInputStream(delimiter + sample, StandardCharsets.UTF_8);
    }
}
