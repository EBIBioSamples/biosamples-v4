/*
* Copyright 2019 EMBL - European Bioinformatics Institute
* Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
* file except in compliance with the License. You may obtain a copy of the License at
* http://www.apache.org/licenses/LICENSE-2.0
* Unless required by applicable law or agreed to in writing, software distributed under the
* License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
* CONDITIONS OF ANY KIND, either express or implied. See the License for the
* specific language governing permissions and limitations under the License.
*/
package uk.ac.ebi.biosamples.certification.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.Collections;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import uk.ac.ebi.biosamples.model.certification.*;
import uk.ac.ebi.biosamples.service.certification.*;

@RunWith(SpringRunner.class)
@SpringBootTest(
    classes = {
      FileRecorder.class,
      Curator.class,
      Certifier.class,
      ConfigLoader.class,
      Validator.class,
      Applicator.class,
      NullRecorder.class
    },
    properties = {"job.autorun.enabled=false"})
public class FileRecorderTest {
  @Qualifier("nullRecorder")
  @Autowired
  private Recorder recorder;

  @Test
  public void given_certificate_save_to_file() throws IOException {
    String data =
        IOUtils.toString(
            getClass().getClassLoader().getResourceAsStream("json/ncbi-SAMN03894263.json"), "UTF8");
    SampleDocument sampleDocument = new SampleDocument("test-uuid", data);
    Checklist checklist =
        new Checklist("ncbi", "0.0.1", "schemas/certification/ncbi-candidate-schema.json", false);
    Certificate certificate = new Certificate(sampleDocument, Collections.emptyList(), checklist);
    CertificationResult certificationResult =
        new CertificationResult(sampleDocument.getAccession());
    certificationResult.add(certificate);
    BioSamplesCertificationComplainceResult bioSamplesCertificationComplainceResult =
        recorder.record(Collections.singleton(certificationResult));
    assertNotNull(bioSamplesCertificationComplainceResult);
    assertEquals(certificate, bioSamplesCertificationComplainceResult.getCertificates().get(0));
  }

  @Test(expected = IllegalArgumentException.class)
  public void given_null_certificate_throw_exception() throws IOException {
    recorder.record(null);
  }
}
