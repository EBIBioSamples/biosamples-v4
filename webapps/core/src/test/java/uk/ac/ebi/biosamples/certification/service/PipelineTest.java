/*
package uk.ac.ebi.biosamples.certification.service;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import uk.ac.ebi.biosamples.model.certification.BioSamplesCertificationComplainceResult;
import uk.ac.ebi.biosamples.mongo.repo.MongoCurationLinkRepository;
import uk.ac.ebi.biosamples.service.CurationPersistService;
import uk.ac.ebi.biosamples.service.certification.*;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {CertifyService.class, Interrogator.class, FileRecorder.class,
        Curator.class, Certifier.class, ConfigLoader.class, Validator.class, Applicator.class, Identifier.class,
        NullRecorder.class, CurationPersistService.class, MongoCurationLinkRepository.class},
        properties = {"job.autorun.enabled=false"})
public class PipelineTest {
    @Autowired
    private CertifyService pipeline;

    @Test
    public void given_ncbi_sample_run_pipeline_for_SAMN03894263() throws IOException {
        String data = IOUtils.toString(getClass().getClassLoader().getResourceAsStream("json/ncbi-SAMN03894263.json"), "UTF8");
        BioSamplesCertificationComplainceResult rr = pipeline.run(data);
        assertEquals(3, rr.getCertificates().size());
    }

    @Test
    public void given_ncbi_sample_run_pipeline_for_SAMN03894261() throws IOException {
        String data = IOUtils.toString(getClass().getClassLoader().getResourceAsStream("json/ncbi-SAMN03894261.json"), "UTF8");
        BioSamplesCertificationComplainceResult rr = pipeline.run(data);
        assertEquals(3, rr.getCertificates().size());
    }

    @Test
    public void given_ncbi_sample_run_pipeline_for_SAMD00141632() throws IOException {
        String data = IOUtils.toString(getClass().getClassLoader().getResourceAsStream("json/ncbi-SAMD00141632.json"), "UTF8");
        BioSamplesCertificationComplainceResult rr = pipeline.run(data);
        assertEquals(3, rr.getCertificates().size());
    }

    @Test
    public void given_ncbi_sample_run_pipeline_for_SAMD00000001() throws IOException {
        String data = IOUtils.toString(getClass().getClassLoader().getResourceAsStream("json/ncbi-SAMD00000001.json"), "UTF8");
        BioSamplesCertificationComplainceResult rr = pipeline.run(data);
        assertEquals(3, rr.getCertificates().size());
        assertNotEquals(rr.getCertificates().get(0).getChecklist(), rr.getCertificates().get(1).getChecklist());
    }

    @Test
    public void given_ncbi_sample_run_pipeline_for_SAMD00000001_non_pretty() throws IOException {
        String data = IOUtils.toString(getClass().getClassLoader().getResourceAsStream("json/ncbi-SAMD00000001-non-pretty.json"), "UTF8");
        BioSamplesCertificationComplainceResult rr = pipeline.run(data);
        assertEquals(3, rr.getCertificates().size());
        assertNotEquals(rr.getCertificates().get(0).getChecklist(), rr.getCertificates().get(1).getChecklist());
    }
}

*/
