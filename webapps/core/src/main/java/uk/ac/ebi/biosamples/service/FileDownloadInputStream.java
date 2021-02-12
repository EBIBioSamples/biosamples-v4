package uk.ac.ebi.biosamples.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.commons.io.IOUtils;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.filter.Filter;
import uk.ac.ebi.biosamples.solr.repo.CursorArrayList;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.Queue;

public class FileDownloadInputStream extends InputStream {
    private final SamplePageService samplePageService;
    private final String text;
    private final Collection<Filter> filters;
    private final Collection<String> domains;

    private final ObjectMapper objectMapper;
    private final Queue<Sample> sampleQueue;
    private InputStream sampleStream;
    private String cursor;
    private boolean eof;

    private static final String START_OF_CONTENT = "[" + System.lineSeparator();
    private static final String END_OF_CONTENT = "]";
    private static final String DELIMITER = "," + System.lineSeparator();

    public FileDownloadInputStream(SamplePageService samplePageService, String text, Collection<Filter> filters, Collection<String> domains) throws IOException {
        this.samplePageService = samplePageService;
        this.text = text;
        this.filters = filters;
        this.domains = domains;

        objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        sampleQueue = new LinkedList<>();
        cursor = "*";
        eof = false;
    }


//    @Override
    public int read1() throws IOException {
        if (sampleStream == null) {
            CursorArrayList<Sample> samplePage = samplePageService.getSamplesByText(text, filters, domains, cursor, 1000, null);
            sampleQueue.addAll(samplePage);
            cursor = samplePage.getNextCursorMark();

            Sample sample = sampleQueue.poll();
            String jsonSample = objectMapper.writeValueAsString(sample);
            // todo add [ at the beginning
            sampleStream = IOUtils.toInputStream(START_OF_CONTENT + jsonSample, "UTF-8");
        }

        int nextByte = sampleStream.read();
        if (nextByte == -1) {
            if (sampleQueue.isEmpty()) {
                if (eof) {
                    return -1;
                }

                CursorArrayList<Sample> samplePage = samplePageService.getSamplesByText(text, filters, domains, cursor, 1000, null);
                if (samplePage.isEmpty()) {
                    sampleStream = IOUtils.toInputStream(END_OF_CONTENT, "UTF-8");
                    eof = true;
                } else {
                    sampleQueue.addAll(samplePage);
                    cursor = samplePage.getNextCursorMark();

                    Sample sample = sampleQueue.poll();
                    String jsonSample = objectMapper.writeValueAsString(sample);
                    sampleStream = IOUtils.toInputStream(DELIMITER + jsonSample, "UTF-8");
                }
            } else {
                Sample sample = sampleQueue.poll();
                String jsonSample = objectMapper.writeValueAsString(sample);
                sampleStream = IOUtils.toInputStream(DELIMITER + jsonSample, "UTF-8");
            }

            nextByte = sampleStream.read();
        }

        return nextByte;
    }

    public int read() throws IOException {
        if (sampleStream == null) {
            sampleStream = generateStream(START_OF_CONTENT);
        }

        int nextByte = sampleStream.read();
        if (nextByte == -1) {
            if (cursor != null) {
                sampleStream = generateStream(DELIMITER);
                nextByte = sampleStream.read();
            }
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
            inputStream = toInputStream(delimiter, objectMapper.writeValueAsString(sample));
        } else {
            inputStream = toInputStream(END_OF_CONTENT, "");
        }

        return inputStream;
    }

    private void loadSamples() {
        CursorArrayList<Sample> samplePage = samplePageService.getSamplesByText(text, filters, domains, cursor, 1000, null);
        if (!samplePage.isEmpty()) {
            sampleQueue.addAll(samplePage);
            cursor = samplePage.getNextCursorMark();
        } else {
            cursor = null; // mark end of samples
        }
    }

    private InputStream toInputStream(String delimiter, String sample) throws IOException {
        return IOUtils.toInputStream(delimiter + sample, "UTF-8");
    }
}
