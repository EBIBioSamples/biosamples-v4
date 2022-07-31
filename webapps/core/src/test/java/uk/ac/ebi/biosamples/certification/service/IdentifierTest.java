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

import static junit.framework.TestCase.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;
import uk.ac.ebi.biosamples.BioSamplesProperties;
import uk.ac.ebi.biosamples.model.certification.SampleDocument;
import uk.ac.ebi.biosamples.service.certification.*;
import uk.ac.ebi.biosamples.service.certification.Validator;
import uk.ac.ebi.biosamples.validation.ElixirSchemaValidator;
import uk.ac.ebi.biosamples.validation.ValidatorI;

@RunWith(SpringRunner.class)
@SpringBootTest(
    classes = {
      Identifier.class,
      Curator.class,
      Certifier.class,
      ConfigLoader.class,
      Validator.class,
      ValidatorI.class,
      ElixirSchemaValidator.class,
      RestTemplate.class,
      BioSamplesProperties.class,
      ObjectMapper.class,
      Applicator.class
    },
    properties = {"job.autorun.enabled=false"})
public class IdentifierTest {
  @Autowired private Identifier identifier;
  @MockBean ElixirSchemaValidator validator;

  @Test
  public void given_ncbi_sample_return_sample() throws IOException {
    String data =
        IOUtils.toString(
            getClass().getClassLoader().getResourceAsStream("json/ncbi-SAMN03894263.json"), "UTF8");
    SampleDocument sampleDocument = identifier.identify(data);
    assertTrue(sampleDocument.getAccession().matches("SAM[END][AG]?[0-9]+"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void given_null_sample_throw_exception() throws IOException {
    identifier.identify(null);
  }
}
