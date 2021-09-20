/*
* Copyright 2021 EMBL - European Bioinformatics Institute
* Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
* file except in compliance with the License. You may obtain a copy of the License at
* http://www.apache.org/licenses/LICENSE-2.0
* Unless required by applicable law or agreed to in writing, software distributed under the
* License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
* CONDITIONS OF ANY KIND, either express or implied. See the License for the
* specific language governing permissions and limitations under the License.
*/
package uk.ac.ebi.biosamples.certification.service;

import static junit.framework.TestCase.assertNotNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Collections;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;
import uk.ac.ebi.biosamples.BioSamplesProperties;
import uk.ac.ebi.biosamples.model.certification.*;
import uk.ac.ebi.biosamples.service.certification.*;
import uk.ac.ebi.biosamples.service.certification.Validator;
import uk.ac.ebi.biosamples.validation.ElixirSchemaValidator;
import uk.ac.ebi.biosamples.validation.ValidatorI;

@RunWith(SpringRunner.class)
@SpringBootTest(
    classes = {
      Certifier.class,
      ConfigLoader.class,
      Validator.class,
      ValidatorI.class,
      ElixirSchemaValidator.class,
      RestTemplate.class,
      BioSamplesProperties.class,
      ObjectMapper.class,
      Applicator.class,
      CertifyService.class,
      Curator.class,
      Identifier.class,
      Interrogator.class,
      NullRecorder.class
    },
    properties = {"job.autorun.enabled=false"})
public class CertifierTest {
  @Autowired private Certifier certifier;
  @Autowired private CertifyService certifyService;
  @MockBean ElixirSchemaValidator validator;

  @Test
  public void given_valid_plan_result_issue_certificate() throws Exception {
    Mockito.doNothing().when(validator).validate(Mockito.anyString(), Mockito.anyString());

    String data =
        IOUtils.toString(
            getClass().getClassLoader().getResourceAsStream("json/ncbi-SAMN03894263-curated.json"),
            "UTF8");
    SampleDocument sampleDocument = new SampleDocument("test-uuid", data);
    Plan plan = new Plan("ncbi-0.0.1", "biosamples-0.0.1", Collections.EMPTY_LIST);
    PlanResult planResult = new PlanResult(sampleDocument, plan);
    CertificationResult certificationResult = certifier.certify(planResult, true);
    assertNotNull(certificationResult);
  }

  @Test
  public void given_valid_plan_result_issue_certificate_curator_test() throws Exception {
    String data =
        IOUtils.toString(
            getClass().getClassLoader().getResourceAsStream("json/ncbi-SAMN03894263-curated.json"),
            "UTF8");
    SampleDocument sampleDocument = new SampleDocument("test-uuid", data);
    final ObjectMapper jsonMapper = new ObjectMapper();

    BioSamplesCertificationComplainceResult result =
        certifyService.recordResult(sampleDocument, true);
  }

  @Test(expected = IllegalArgumentException.class)
  public void given_null_planResult_throw_exception() throws IOException {
    certifier.certify((SampleDocument) null, true);
  }
}
