package uk.ac.ebi.biosamples.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.CharSet;
import org.apache.tomcat.util.bcel.classfile.ConstantUtf8;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.filter.Filter;
import uk.ac.ebi.biosamples.solr.repo.CursorArrayList;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.zip.ZipOutputStream;

@Service
public class FileDownloadService {
    private final SamplePageService samplePageService;
    private final ObjectMapper objectMapper;

    public FileDownloadService(SamplePageService samplePageService, ObjectMapper objectMapper) {
        this.samplePageService = samplePageService;
        this.objectMapper = objectMapper;
    }

    public ZipOutputStream downloadCompressed() {
        return null;
    }

    public InputStream getDownloadStream(String text, Collection<Filter> filters, Collection<String> domains) throws IOException {
        FileDownloadInputStream fileDownloadInputStream = new FileDownloadInputStream(samplePageService, text, filters, domains);
        return fileDownloadInputStream;

//        Pageable pageable = new PageRequest(0, 1000);
//        Page<Sample> samples = samplePageService.getSamplesByText(text, filters, domains, pageable, null);
//        samples.getContent().stream()
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
            return IOUtils.toInputStream(jsonSamples, "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException();
        }

    }


    private ZipOutputStream compressStream() {
        return null;
    }
}
