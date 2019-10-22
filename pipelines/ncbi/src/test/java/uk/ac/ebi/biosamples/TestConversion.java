package uk.ac.ebi.biosamples;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import uk.ac.ebi.biosamples.client.BioSamplesClient;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestApplication.class, properties = {"job.autorun.enabled=false"})
public class TestConversion {

    @Autowired
    public BioSamplesClient bioSamplesClient;

    @Test
    @Ignore
    public void test_with_all_ncbi_samples() throws Exception {
        System.out.println("A");
    }
}
