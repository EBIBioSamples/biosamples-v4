package uk.ac.ebi.biosamples.service;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.Assert;

import java.util.zip.ZipOutputStream;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
public class FileDownloadServiceTest {

    @Autowired
    FileDownloadService fileDownloadService;

    @Test
    public void downloadCompressed() {

//        Assert.notNull(zipOutputStream, "Zip stream should not be null");
    }

}
