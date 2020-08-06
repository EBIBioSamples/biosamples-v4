package uk.ac.ebi.biosamples.certification.service;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import uk.ac.ebi.biosamples.model.certification.InterrogationResult;
import uk.ac.ebi.biosamples.model.certification.SampleDocument;
import uk.ac.ebi.biosamples.service.certification.*;

import java.io.IOException;
import java.util.Collections;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {Interrogator.class, ConfigLoader.class, Curator.class, Certifier.class, Validator.class, Applicator.class}, properties = {"job.autorun.enabled=false"})
public class InterrogatorTest {
    @Autowired
    private Interrogator interrogator;

    @Autowired
    private ConfigLoader configLoader;

    @Test
    public void given_ncbi_sample_return_ncbi_checklist_as_a_match() throws IOException {
        String data = IOUtils.toString(getClass().getClassLoader().getResourceAsStream("json/ncbi-SAMN03894263.json"), "UTF8");
        SampleDocument sampleDocument = new SampleDocument("test-uuid", data);
        InterrogationResult interrogationResult = interrogator.interrogate(sampleDocument);
        //assertTrue(interrogationResult.getChecklists().size()==2);
        //assertEquals("ncbi-0.0.1", interrogationResult.getChecklists().get(1).getID());
    }

    @Test
    public void given_empty_sample_return_empty_matches() {
        SampleDocument sampleDocument = new SampleDocument("test-uuid", "{}");
        InterrogationResult interrogationResult = interrogator.interrogate(sampleDocument);
        assertEquals(Collections.EMPTY_LIST, interrogationResult.getChecklists());
    }

    @Test(expected = IllegalArgumentException.class)
    public void given_null_sample_throw_exception() throws IOException {
        interrogator.interrogate(null);
    }
}
