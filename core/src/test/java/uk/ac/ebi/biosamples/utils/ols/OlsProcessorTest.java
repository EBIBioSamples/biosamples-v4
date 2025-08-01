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
package uk.ac.ebi.biosamples.utils.ols;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.google.common.collect.ImmutableMap;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.Optional;
import org.junit.Before;
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

@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestApplication.class)
@AutoConfigureWebClient
public class OlsProcessorTest {

  private MockRestServiceServer mockServer;

  @Autowired private OlsProcessor olsProcessor;

  @Autowired private RestTemplate restTemplate;

  @Before
  public void setUp() {
    mockServer = MockRestServiceServer.createServer(restTemplate);
  }

  private final Map<String, String> expectedValues =
      ImmutableMap.of(
          "NCIT:C2985", "http://purl.obolibrary.org/obo/NCIT_C2985",
          "UBERON:0003978", "http://purl.obolibrary.org/obo/UBERON_0003978",
          "FOODON:03304708", "http://purl.obolibrary.org/obo/FOODON_03304708",
          "NCIT_C14207", "http://purl.obolibrary.org/obo/NCIT_C14207");

  @Test
  public void test_OlsProcessor_returns_correct_value_for_invalid_ols_term() throws IOException {
    final String shortcode = "invalid-term";
    final Optional<String> result = performQuery(shortcode);
    assertFalse(result.isPresent());
  }

  private String readFile(final String filePath) throws IOException {
    final InputStream inputStream = OlsProcessorTest.class.getResourceAsStream(filePath);
    final StringBuilder resultStringBuilder = new StringBuilder();
    try (final BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
      String line;
      while ((line = br.readLine()) != null) {
        resultStringBuilder.append(line).append("\n");
      }
    }
    return resultStringBuilder.toString();
  }

  private Optional<String> performQuery(final String shortcode) throws IOException {
    final String expectedResponse = readFile("/ols-responses/" + shortcode + ".json");
    mockServer.reset();
    mockServer
        .expect(requestTo("https://www.ebi.ac.uk/ols/api/terms?id=" + shortcode + "&size=500"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess(expectedResponse, MediaType.APPLICATION_JSON));
    final Optional<String> result = olsProcessor.queryOlsForShortcode(shortcode);
    mockServer.verify();
    return result;
  }

  @Test
  public void test_OlsProcessor_returns_correct_value_for_multi_ols_term() throws IOException {
    final String shortcode = "PATO_0000384";
    final Optional<String> result = performQuery(shortcode);
    assertTrue(result.isPresent());
    assertEquals("http://purl.obolibrary.org/obo/PATO_0000384", result.get());
  }

  @Test
  public void
      test_OlsProcessor_returns_correct_value_for_standard_ols_term_that_has_defining_ontology()
          throws IOException {
    final String shortcode = "NCBITaxon_3702";
    final Optional<String> result = performQuery(shortcode);
    assertTrue(result.isPresent());
    assertEquals("http://purl.obolibrary.org/obo/NCBITaxon_3702", result.get());
  }

  @Test
  public void
      test_OlsProcessor_returns_correct_value_for_standard_ols_term_does_not_have_defining_ontology()
          throws IOException {
    final String shortcode = "FBcv_0003016";
    final Optional<String> result = performQuery(shortcode);
    assertTrue(result.isPresent());
    assertEquals("http://purl.obolibrary.org/obo/FBcv_0003016", result.get());
  }
  /*@Test
  public void test_OlsProcessor_returns_correct_value_for_example_terms() throws IOException {
      for (Map.Entry<String, String> entry : expectedValues.entrySet()) {
          Optional<String> result = performQuery(entry.getKey());
          assertTrue(result.isPresent());
          assertEquals(entry.getValue(), result.get());
      }
  }*/
}
