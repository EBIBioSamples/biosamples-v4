package uk.ac.ebi.biosamples.certification.service;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import uk.ac.ebi.biosamples.model.certification.*;
import uk.ac.ebi.biosamples.service.certification.Applicator;

import java.io.IOException;
import java.util.Collections;

import static org.junit.Assert.assertEquals;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = Applicator.class, properties = {"job.autorun.enabled=false"})
public class ApplicatorTest {
    @Autowired
    private Applicator applicator;

    @Test
    public void given_valid_plan_result_apply_curations() throws Exception {
        applyCuration("json/ncbi-SAMN03894263.json", "json/ncbi-SAMN03894263-curated.json");
    }

    private void applyCuration(String source, String expectedResult) throws IOException {
        String data = IOUtils.toString(getClass().getClassLoader().getResourceAsStream(source), "UTF8");
        String curatedData = IOUtils.toString(getClass().getClassLoader().getResourceAsStream(expectedResult), "UTF8");
        SampleDocument sampleDocument = new SampleDocument("test", data);
        SampleDocument curatedSampleDocument = new SampleDocument("test", curatedData);
        Curation curation = new Curation("INSDC status", "public");
        Plan plan = new Plan("ncbi-0.0.1", "biosamples-0.0.1", Collections.singletonList(curation));
        PlanResult planResult = new PlanResult(sampleDocument, plan);
        planResult.addCurationResult(new CurationResult(curation.getCharacteristic(), "live", curation.getValue()));
        assertEquals(curatedSampleDocument.getDocument().trim(), applicator.apply(planResult).getDocument().trim());
    }

    @Test(expected = IllegalArgumentException.class)
    public void given_null_planResult_throw_exception() throws IOException {
        applicator.apply(null);
    }
}
