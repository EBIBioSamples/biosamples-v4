package uk.ac.ebi.biosamples.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.filter.Filter;
import uk.ac.ebi.biosamples.solr.repo.CursorArrayList;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class FileDownloadService {
    private Logger log = LoggerFactory.getLogger(getClass());

    private final SamplePageService samplePageService;
    private final ObjectMapper objectMapper;

    public FileDownloadService(SamplePageService samplePageService, ObjectMapper objectMapper) {
        this.samplePageService = samplePageService;
        this.objectMapper = objectMapper;
    }

    public InputStream getDownloadStream(String text, Collection<Filter> filters, Collection<String> domains, String format) {
        FileDownloadSerializer fileDownloadSerializer;
        if ("application/xml".equals(format)) {
            fileDownloadSerializer = FileDownloadSerializer.getXmlSerializer();
        } else {
            fileDownloadSerializer = FileDownloadSerializer.getJsonSerializer();
        }
        return new FileDownloadInputStream(samplePageService, text, filters, domains);
    }

    public void copyCompressedStream(InputStream in, OutputStream out, String algo) throws IOException {
        switch (algo) {
            case "gzip":
                gzip(in, out);
                break;
            case "deflate":
                deflate(in, out);
                break;
            case "zip":
                zip(in, out);
                break;
            default:
                StreamUtils.copy(in, out);
                break;
        }
    }

    private void gzip(InputStream in, OutputStream out) throws IOException {
        try (ZipOutputStream zippedOut = new ZipOutputStream(out)) {
            ZipEntry zipEntry = new ZipEntry("samples.json");
            zippedOut.putNextEntry(zipEntry);
            StreamUtils.copy(in, zippedOut);

            zippedOut.closeEntry();
            zippedOut.finish();
        }
    }

    private void deflate(InputStream in, OutputStream out) throws IOException {
        try (ZipOutputStream zippedOut = new ZipOutputStream(out)) {
            ZipEntry zipEntry = new ZipEntry("samples.json");
            zippedOut.putNextEntry(zipEntry);
            StreamUtils.copy(in, zippedOut);

            zippedOut.closeEntry();
            zippedOut.finish();
        }
    }

    private void zip(InputStream in, OutputStream out) throws IOException {
        try (ZipOutputStream zippedOut = new ZipOutputStream(out)) {
            ZipEntry zipEntry = new ZipEntry("samples.json");
            zippedOut.putNextEntry(zipEntry);
            StreamUtils.copy(in, zippedOut);

            zippedOut.closeEntry();
            zippedOut.finish();
        }
    }


    public InputStream writeStringToStream(String text, Collection<Filter> filters, Collection<String> domains) {
        List<Sample> samples = new ArrayList<>();
        CursorArrayList<Sample> samplePage = samplePageService.getSamplesByText(text, filters, domains, "*", 1000, null);
        while (!samplePage.isEmpty()) {
            samples.addAll(samplePage);
            samplePage = samplePageService.getSamplesByText(text, filters, domains, samplePage.getNextCursorMark(), 1000, null);
        }

        try {
            String jsonSamples = objectMapper.writeValueAsString(samples);
            return IOUtils.toInputStream(jsonSamples, StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException();
        }

    }
}
