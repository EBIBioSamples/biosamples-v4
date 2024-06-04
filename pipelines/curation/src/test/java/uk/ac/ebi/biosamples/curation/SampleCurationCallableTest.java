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
package uk.ac.ebi.biosamples.curation;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static uk.ac.ebi.biosamples.curation.SampleCurationCallable.NON_APPLICABLE_SYNONYMS;
import static uk.ac.ebi.biosamples.curation.SampleCurationCallable.isNotApplicableSynonym;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Set;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import uk.ac.ebi.biosamples.curation.service.IriUrlValidatorService;
import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.ols.OlsProcessor;
import uk.ac.ebi.biosamples.service.CurationApplicationService;

@Ignore
@RunWith(SpringRunner.class)
@SpringBootTest(
    classes = TestApplication.class,
    properties = {"job.autorun.enabled=false"})
@AutoConfigureWebClient
public class SampleCurationCallableTest {
  @Autowired private ObjectMapper objectMapper;
  @Autowired private OlsProcessor olsProcessor;
  @Autowired private RestTemplate restTemplate;
  @Autowired private MockBioSamplesClient mockBioSamplesClient;
  @Autowired private CurationApplicationService curationApplicationService;
  private MockRestServiceServer mockServer;

  @Before
  public void setUp() {
    mockServer = MockRestServiceServer.createServer(restTemplate);
  }

  @Test
  public void given_known_incorrect_sample_ensure_curated_correctly() throws Exception {
    final String filePath = "/examples/samples/SAMEA103887543.json";
    final Sample sample =
        objectMapper.readValue(
            SampleCurationCallableTest.class.getResourceAsStream(filePath), Sample.class);
    final String shortcode = "PATO_0000384";
    final String expectedResponse = readFile("/examples/ols-responses/" + shortcode + ".json");
    mockServer
        .expect(requestTo("https://www.ebi.ac.uk/ols/api/terms?id=" + shortcode + "&size=500"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess(expectedResponse, MediaType.APPLICATION_JSON));
    final SampleCurationCallable sampleCurationCallable =
        new SampleCurationCallable(
            mockBioSamplesClient,
            sample,
            olsProcessor,
            curationApplicationService,
            null,
            new IriUrlValidatorService());
    sampleCurationCallable.call();
    final Sample curatedSample =
        curationApplicationService.applyAllCurationToSample(
            sample, mockBioSamplesClient.getCurations(sample.getAccession()));
    final String curatedFilePath = "/examples/samples/SAMEA103887543-curated.json";
    final Sample expectedCuratedSample =
        objectMapper.readValue(
            SampleCurationCallableTest.class.getResourceAsStream(curatedFilePath), Sample.class);
    assertEquals(expectedCuratedSample, curatedSample);
  }

  @Test
  public void given_sample_ensure_Ancestory_is_removed() throws Exception {
    final String filePath = "/examples/samples/SAMEA5327217.json";
    final String attributeName = "Ancestory";
    final Sample sample =
        objectMapper.readValue(
            SampleCurationCallableTest.class.getResourceAsStream(filePath), Sample.class);
    assertTrue(hasAttribute(sample.getAttributes(), attributeName));
    final SampleCurationCallable sampleCurationCallable =
        new SampleCurationCallable(
            mockBioSamplesClient,
            sample,
            olsProcessor,
            curationApplicationService,
            null,
            new IriUrlValidatorService());
    sampleCurationCallable.call();
    final Sample curatedSample =
        curationApplicationService.applyAllCurationToSample(
            sample, mockBioSamplesClient.getCurations(sample.getAccession()));
    assertFalse(hasAttribute(curatedSample.getAttributes(), attributeName));
  }

  @Test
  public void given_sample_ensure_organism_is_not_removed() throws Exception {
    final String filePath = "/examples/samples/SAMEA5327217.json";
    final String attributeName = "Organism";
    final Sample sample =
        objectMapper.readValue(
            SampleCurationCallableTest.class.getResourceAsStream(filePath), Sample.class);
    assertTrue(hasAttribute(sample.getAttributes(), attributeName));
    final SampleCurationCallable sampleCurationCallable =
        new SampleCurationCallable(
            mockBioSamplesClient,
            sample,
            olsProcessor,
            curationApplicationService,
            null,
            new IriUrlValidatorService());
    sampleCurationCallable.call();
    final Sample curatedSample =
        curationApplicationService.applyAllCurationToSample(
            sample, mockBioSamplesClient.getCurations(sample.getAccession()));
    assertTrue(hasAttribute(curatedSample.getAttributes(), attributeName));
  }

  private boolean hasAttribute(final Set<Attribute> attributes, final String name) {
    boolean found = false;
    for (final Attribute attribute : attributes) {
      if (attribute.getType() == name) {
        found = true;
        break;
      }
    }
    return found;
  }

  private String readFile(final String filePath) throws IOException {
    final InputStream inputStream = SampleCurationCallableTest.class.getResourceAsStream(filePath);
    final StringBuilder resultStringBuilder = new StringBuilder();
    try (final BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
      String line;
      while ((line = br.readLine()) != null) {
        resultStringBuilder.append(line).append("\n");
      }
    }
    return resultStringBuilder.toString();
  }

  @Test
  public void test_non_applicable_synonyms() {
    for (final String nonApplicableSynonyms : NON_APPLICABLE_SYNONYMS) {
      assertTrue(isNotApplicableSynonym(nonApplicableSynonyms));
    }
    assertTrue(isNotApplicableSynonym("not_applicable"));
    assertFalse(isNotApplicableSynonym("rubbish"));
  }
}
