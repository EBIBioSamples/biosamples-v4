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

import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
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
public class CuratorTest {
  @Autowired private Curator curator;
  @MockBean ElixirSchemaValidator validator;

  @Test
  public void given_ChecklistMatches_run_curation_plans() throws Exception {
    List<Checklist> checklistList = new ArrayList<>();
    checklistList =
        Collections.singletonList(
            new Checklist(
                "ncbi-candidate-schema",
                "0.0.1",
                "schemas/certification/ncbi-candidate-schema.json",
                false));
    InterrogationResult interrogationResult = new InterrogationResult(testSample(), checklistList);
    List<PlanResult> planResults = curator.runCurationPlans(interrogationResult);
    for (PlanResult planResult : planResults) {
      assertNotNull(planResult.getSampleDocument());
      assertFalse(planResult.getCurationResults().isEmpty());
      assertEquals("live", planResult.getCurationResults().get(0).getBefore());
      assertEquals("public", planResult.getCurationResults().get(0).getAfter());
    }
  }

  @Test
  public void given_ChecklistNotMatches_run_Recommendation() throws Exception {
    List<Checklist> checklistList;
    checklistList =
        Collections.singletonList(
            new Checklist(
                "biosamples-minimal",
                "0.0.1",
                "schemas/certification/biosamples-minimal.json",
                false));
    InterrogationResult interrogationResult = new InterrogationResult(testSample(), checklistList);
    List<Recommendation> recommendations = curator.runRecommendations(interrogationResult);

    recommendations.forEach(
        recommendation -> {
          assertTrue(recommendation.getSuggestions() != null);
        });
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
  public void given_sample_and_no_checklists_checklistMatches_return_empty_plan_results()
      throws IOException {
    InterrogationResult interrogationResult =
        new InterrogationResult(testSample(), Collections.EMPTY_LIST);
    assertTrue(curator.runCurationPlans(interrogationResult).isEmpty());
  }

  private SampleDocument testSample() throws IOException {
    String data =
        IOUtils.toString(
            getClass().getClassLoader().getResourceAsStream("json/ncbi-SAMN03894263.json"), "UTF8");
    SampleDocument sampleDocument = new SampleDocument("test", data);
    return sampleDocument;
  }
}
