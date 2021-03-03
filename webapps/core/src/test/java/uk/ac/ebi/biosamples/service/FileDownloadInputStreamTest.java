package uk.ac.ebi.biosamples.service;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.filter.Filter;
import uk.ac.ebi.biosamples.solr.repo.CursorArrayList;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.time.Instant;
import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class FileDownloadInputStreamTest {
    @Mock
    private SamplePageService samplePageService;

    private FileDownloadInputStream fileDownloadInputStream;

    int pageSize = 1000;
    int sampleCount = 100;
    String cursor = "*";
    String sampleSearchText = "";
    String emptySamplesText = "no samples search";
    Collection<Filter> filters = Collections.emptyList();
    Collection<String> domains = Collections.emptyList();

    @Before
    public void init() {
        CursorArrayList<Sample> samplePage = new CursorArrayList<>(cursor);
        when(samplePageService.getSamplesByText(emptySamplesText, filters, domains, cursor, pageSize, null))
                .thenReturn(samplePage);

        CursorArrayList<Sample> samplePageWithSample = new CursorArrayList<>(cursor);
        samplePageWithSample.add(getTestSample());
        when(samplePageService.getSamplesByText(sampleSearchText, filters, domains, cursor, pageSize, null))
                .thenReturn(samplePageWithSample);
    }

    @Test
    public void read() throws IOException {
        FileDownloadSerializer serializer = FileDownloadSerializer.getSerializerFor("json");
        fileDownloadInputStream = new FileDownloadInputStream(samplePageService, emptySamplesText, filters, sampleCount, domains, serializer);

        int startByte = fileDownloadInputStream.read();
        assertTrue(startByte > 0);
    }

    @Test
    public void read_empty_json() throws IOException {
        FileDownloadSerializer serializer = FileDownloadSerializer.getSerializerFor("json");
        fileDownloadInputStream = new FileDownloadInputStream(samplePageService, emptySamplesText, filters, sampleCount, domains, serializer);

        StringWriter writer = new StringWriter();
        IOUtils.copy(fileDownloadInputStream, writer, Charset.defaultCharset());
        String emptyJson = writer.toString();
        assertEquals("[]", emptyJson);
    }

    @Test
    public void read_empty_xml() throws IOException {
        FileDownloadSerializer serializer = FileDownloadSerializer.getSerializerFor("xml");
        fileDownloadInputStream = new FileDownloadInputStream(samplePageService, emptySamplesText, filters, sampleCount, domains, serializer);

        StringWriter writer = new StringWriter();
        IOUtils.copy(fileDownloadInputStream, writer, Charset.defaultCharset());
        String emptyJson = writer.toString();
        assertEquals("<BioSamples>\n</BioSamples>", emptyJson);
    }

    @Test
    public void read_json_with_samples() throws IOException {
        FileDownloadSerializer serializer = FileDownloadSerializer.getSerializerFor("json");
        fileDownloadInputStream = new FileDownloadInputStream(samplePageService, sampleSearchText, filters, sampleCount, domains, serializer);

        StringWriter writer = new StringWriter();
        IOUtils.copy(fileDownloadInputStream, writer, Charset.defaultCharset());
        String sampleJson = writer.toString();
        assertNotEquals("[]", sampleJson);
    }

    private Sample getTestSample() {
        String name = "FileDownloadInputStreamTest_sample";
        String accession = "fileDownloadTestAccession";
        Instant release = Instant.parse("2016-04-01T11:36:57.00Z");
        SortedSet<Attribute> attributes = new TreeSet<>();
        attributes.add(Attribute.build("organism", "Homo sapiens"));
        attributes.add(Attribute.build("organism_part", "liver"));

        return new Sample.Builder(name)
                .withAccession(accession)
                .withDomain("self.biosamplesUnitTests")
                .withRelease(release)
                .withAttributes(attributes)
                .build();
    }
}