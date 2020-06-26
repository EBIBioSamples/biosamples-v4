package uk.ac.ebi.biosamples.certification.service;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import uk.ac.ebi.biosamples.model.certification.CertificationResult;
import uk.ac.ebi.biosamples.model.certification.Plan;
import uk.ac.ebi.biosamples.model.certification.PlanResult;
import uk.ac.ebi.biosamples.model.certification.Sample;
import uk.ac.ebi.biosamples.service.certification.Applicator;
import uk.ac.ebi.biosamples.service.certification.Certifier;
import uk.ac.ebi.biosamples.service.certification.ConfigLoader;
import uk.ac.ebi.biosamples.service.certification.Validator;

import java.io.IOException;
import java.util.Collections;

import static junit.framework.TestCase.assertNotNull;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {Certifier.class, ConfigLoader.class, Validator.class, Applicator.class}, properties = {"job.autorun.enabled=false"})
public class CertifierTest {

    @Autowired
    private Certifier certifier;

    @Test
    public void given_valid_plan_result_issue_certificate() throws Exception {
        String data = IOUtils.toString(getClass().getClassLoader().getResourceAsStream("json/ncbi-SAMN03894263-curated.json"), "UTF8");
        Sample sample = new Sample("test-uuid", data);
        Plan plan = new Plan("ncbi-0.0.1", "biosamples-0.0.1", Collections.EMPTY_LIST);
        PlanResult planResult = new PlanResult(sample, plan);
        CertificationResult certificationResult = certifier.certify(planResult);
        assertNotNull(certificationResult);
    }

    @Test(expected = IllegalArgumentException.class)
    public void given_null_planResult_throw_exception() throws IOException {
        certifier.certify((Sample) null);
    }
}
