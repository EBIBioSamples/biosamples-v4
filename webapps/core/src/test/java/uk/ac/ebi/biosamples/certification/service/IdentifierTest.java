package uk.ac.ebi.biosamples.certification.service;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import uk.ac.ebi.biosamples.model.certification.SampleDocument;
import uk.ac.ebi.biosamples.service.certification.*;

import java.io.IOException;

import static junit.framework.TestCase.assertTrue;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {Identifier.class, Curator.class, Certifier.class, ConfigLoader.class, Validator.class, Applicator.class}, properties = {"job.autorun.enabled=false"})
public class IdentifierTest {
    @Autowired
    private Identifier identifier;

    @Test
    public void given_ncbi_sample_return_sample() throws IOException {
        String data = IOUtils.toString(getClass().getClassLoader().getResourceAsStream("json/ncbi-SAMN03894263.json"), "UTF8");
        SampleDocument sampleDocument = identifier.identify(data);
        assertTrue(sampleDocument.getAccession().matches("SAM[END][AG]?[0-9]+"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void given_null_sample_throw_exception() throws IOException {
        identifier.identify(null);
    }

}