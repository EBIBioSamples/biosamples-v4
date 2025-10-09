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
package uk.ac.ebi.biosamples;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;
import java.util.Optional;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import uk.ac.ebi.biosamples.core.model.Attribute;
import uk.ac.ebi.biosamples.core.model.Sample;
import uk.ac.ebi.biosamples.security.model.AuthToken;
import uk.ac.ebi.biosamples.security.model.AuthorizationProvider;
import uk.ac.ebi.biosamples.security.service.AccessControlService;
import uk.ac.ebi.biosamples.service.SampleService;
import uk.ac.ebi.biosamples.service.WebinAuthenticationService;

@RunWith(SpringRunner.class)
@SpringBootTest(properties = {"spring.cloud.gcp.project-id=no_project"})
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class EtagTests {
  private static final String WEBIN_TESTING_ACCOUNT = "WEBIN-12345";
  @Autowired private MockMvc mockMvc;
  @MockBean private WebinAuthenticationService webinAuthenticationService;
  @MockBean private SampleService sampleService;
  @MockBean private AccessControlService accessControlService;

  @Test
  @Ignore
  public void get_validation_endpoint_return_not_allowed_response() throws Exception {
    final String sampleAccession = "SAMEA123456789";
    final Sample testSample =
        new Sample.Builder("TestSample", sampleAccession)
            .withWebinSubmissionAccountId(WEBIN_TESTING_ACCOUNT)
            .addAttribute(new Attribute.Builder("Organism", "Homo sapiens").build())
            .build();

    when(sampleService.fetch(sampleAccession, true)).thenReturn(Optional.of(testSample));
    when(webinAuthenticationService.handleWebinUserSubmission(
            testSample, WEBIN_TESTING_ACCOUNT, Optional.empty()))
        .thenReturn(testSample);
    // doNothing().when(bioSamplesWebinAuthenticationService).isSampleAccessible(isA(Sample.class),
    // "Webin-12345");
    when(accessControlService.extractToken(anyString()))
        .thenReturn(
            Optional.of(
                new AuthToken(
                    "RS256",
                    AuthorizationProvider.WEBIN,
                    WEBIN_TESTING_ACCOUNT,
                    Collections.emptyList())));

    mockMvc
        .perform(
            get("/samples/{accession}", sampleAccession)
                .accept(MediaTypes.HAL_JSON)
                .header("Authorization", "Bearer $TOKEN"))
        .andExpect(status().is2xxSuccessful());

    final MvcResult sampleRequestResult =
        mockMvc
            .perform(get("/samples/{accession}", sampleAccession).accept(MediaTypes.HAL_JSON))
            .andReturn();

    final String etag = sampleRequestResult.getResponse().getHeader("Etag");

    mockMvc
        .perform(
            get("/samples/{accession}", sampleAccession)
                .accept(MediaType.APPLICATION_JSON)
                .header("If-None-Match", etag))
        .andExpect(status().isNotModified());
  }
}
