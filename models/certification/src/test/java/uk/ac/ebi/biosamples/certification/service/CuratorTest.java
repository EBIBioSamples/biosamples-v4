package uk.ac.ebi.biosamples.certification.service;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import uk.ac.ebi.biosamples.model.certification.Checklist;
import uk.ac.ebi.biosamples.model.certification.InterrogationResult;
import uk.ac.ebi.biosamples.model.certification.PlanResult;
import uk.ac.ebi.biosamples.model.certification.Sample;
import uk.ac.ebi.biosamples.service.certification.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {Curator.class, Certifier.class, ConfigLoader.class, Validator.class, Applicator.class}, properties = {"job.autorun.enabled=false"})
public class CuratorTest {

    @Autowired
    private Curator curator;

    @Test
    public void given_ChecklistMatches_run_curation_plans() throws Exception {
        List<Checklist> checklistList = new ArrayList<>();
        checklistList = Collections.singletonList(new Checklist("ncbi", "0.0.1", "schemas/ncbi-candidate-schema.json"));
        InterrogationResult interrogationResult = new InterrogationResult(testSample(), checklistList);
        List<PlanResult> planResults = curator.runCurationPlans(interrogationResult);
        for (PlanResult planResult : planResults) {
            assertNotNull(planResult.getSample());
            assertFalse(planResult.getCurationResults().isEmpty());
            assertEquals("live", planResult.getCurationResults().get(0).getBefore());
            assertEquals("public", planResult.getCurationResults().get(0).getAfter());
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void given_null_checklistMatches_throw_exception() throws IOException {
        curator.runCurationPlans(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void given_null_sample_checklistMatches_throw_exception() throws IOException {
        InterrogationResult interrogationResult = new InterrogationResult(null, Collections.EMPTY_LIST);
        curator.runCurationPlans(interrogationResult);
    }

    @Test
    public void given_sample_and_no_checklists_checklistMatches_return_empty_plan_results() throws IOException {
        InterrogationResult interrogationResult = new InterrogationResult(testSample(), Collections.EMPTY_LIST);
        assertTrue(curator.runCurationPlans(interrogationResult).isEmpty());
    }

    private Sample testSample() throws IOException {
        String data = IOUtils.toString(getClass().getClassLoader().getResourceAsStream("json/ncbi-SAMN03894263.json"), "UTF8");
        Sample sample = new Sample("test", data);
        return sample;
    }
}
