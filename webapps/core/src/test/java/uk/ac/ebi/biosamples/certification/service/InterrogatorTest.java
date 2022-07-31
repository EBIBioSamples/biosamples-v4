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

import static org.junit.Assert.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Collections;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;
import uk.ac.ebi.biosamples.BioSamplesProperties;
import uk.ac.ebi.biosamples.model.certification.InterrogationResult;
import uk.ac.ebi.biosamples.model.certification.SampleDocument;
import uk.ac.ebi.biosamples.service.certification.*;
import uk.ac.ebi.biosamples.service.certification.Validator;
import uk.ac.ebi.biosamples.validation.ElixirSchemaValidator;
import uk.ac.ebi.biosamples.validation.ValidatorI;

@RunWith(SpringRunner.class)
@SpringBootTest(
    classes = {
      Interrogator.class,
      ConfigLoader.class,
      Curator.class,
      Certifier.class,
      Validator.class,
      ValidatorI.class,
      ElixirSchemaValidator.class,
      RestTemplate.class,
      BioSamplesProperties.class,
      ObjectMapper.class,
      Applicator.class
    },
    properties = {"job.autorun.enabled=false"})
public class InterrogatorTest {
  @Autowired private Interrogator interrogator;
  @Autowired private ConfigLoader configLoader;
  @MockBean ElixirSchemaValidator validator;

  @Test
  public void given_ncbi_sample_return_ncbi_checklist_as_a_match() throws IOException {
    String data =
        IOUtils.toString(
            getClass().getClassLoader().getResourceAsStream("json/ncbi-SAMN03894263.json"), "UTF8");
    SampleDocument sampleDocument = new SampleDocument("test-uuid", data);
    InterrogationResult interrogationResult = interrogator.interrogate(sampleDocument);
    // assertTrue(interrogationResult.getChecklists().size()==2);
    // assertEquals("ncbi-0.0.1", interrogationResult.getChecklists().get(1).getID());
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
