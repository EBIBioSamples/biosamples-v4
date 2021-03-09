package uk.ac.ebi.biosamples.service;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.Assert;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(MockitoJUnitRunner.class)
public class FileDownloadServiceTest {
    @Mock
    SamplePageService samplePageService;
    FileDownloadService fileDownloadService;

    @Before
    public void init() {
        fileDownloadService = new FileDownloadService(samplePageService);
    }

    @Test
    public void copyAndCompress() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        InputStream in = new ClassPathResource("amr_sample.json").getInputStream();
        fileDownloadService.copyAndCompress(in, out, true, "json");

        ZipInputStream zipIn = new ZipInputStream(new ByteArrayInputStream(out.toByteArray()));
        ZipEntry entry = zipIn.getNextEntry();
        assertNotNull(entry);
        assertEquals("samples.json", entry.getName());
    }

}
