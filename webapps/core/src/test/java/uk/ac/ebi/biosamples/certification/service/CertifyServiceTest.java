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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;
import uk.ac.ebi.biosamples.BioSamplesProperties;
import uk.ac.ebi.biosamples.model.certification.BioSamplesCertificationComplainceResult;
import uk.ac.ebi.biosamples.model.certification.SampleDocument;
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
public class CertifyServiceTest {
  @Autowired private CertifyService certifyService;
  @MockBean ElixirSchemaValidator validator;

  @Test
  public void given_valid_plan_result_issue_certificate_curator_test() throws Exception {
    String data =
        IOUtils.toString(
            getClass().getClassLoader().getResourceAsStream("json/ncbi-SAMN03894263-curated.json"),
            "UTF8");
    SampleDocument sampleDocument = new SampleDocument("test-uuid", data);

    BioSamplesCertificationComplainceResult result =
        certifyService.recordResult(sampleDocument, true);

    Assert.assertTrue(result.getCertificates().size() == 2);
    Assert.assertTrue(result.getRecommendations().size() == 0);
  }

  @Test
  public void given_valid_plan_result_issue_certificate_curator_test_more() throws Exception {
    String data =
        IOUtils.toString(
            getClass()
                .getClassLoader()
                .getResourceAsStream("json/ncbi-SAMN03894263-uncurated.json"),
            "UTF8");
    SampleDocument sampleDocument = new SampleDocument("test-uuid", data);

    BioSamplesCertificationComplainceResult result =
        certifyService.recordResult(sampleDocument, true);

    Assert.assertTrue(result.getCertificates().size() == 2);
    Assert.assertTrue(result.getRecommendations().size() == 0);
  }
}
