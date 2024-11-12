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
package uk.ac.ebi.biosamples.controller;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import uk.ac.ebi.biosamples.exceptions.GlobalExceptions;
import uk.ac.ebi.biosamples.model.AuthToken;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.auth.AuthorizationProvider;
import uk.ac.ebi.biosamples.service.SampleService;
import uk.ac.ebi.biosamples.service.security.AccessControlService;
import uk.ac.ebi.biosamples.service.security.BioSamplesWebinAuthenticationService;

@SpringBootTest(properties = {"spring.cloud.gcp.project-id=no_project"})
@AutoConfigureMockMvc
public class BulkActionControllerV2Test {
  public static final String WEBIN_TESTING_ACCOUNT = "Webin-12345";
  @Autowired private MockMvc mockMvc;
  @MockBean private SampleService sampleService;
  @MockBean private BioSamplesWebinAuthenticationService bioSamplesWebinAuthenticationService;
  @MockBean private AccessControlService accessControlService;
  @Autowired private ObjectMapper objectMapper;

  @BeforeEach
  public void setUp() {
    // No need for MockitoAnnotations.openMocks(this), as @MockBean takes care of mocking in the
    // Spring context.
  }

  @Test
  public void testAccessionV2_SuccessfulAccession() throws Exception {
    // Arrange
    Sample sample =
        new Sample.Builder("test_1").withWebinSubmissionAccountId(WEBIN_TESTING_ACCOUNT).build();
    Sample accessionedSample =
        new Sample.Builder("test_1")
            .withWebinSubmissionAccountId(WEBIN_TESTING_ACCOUNT)
            .withAccession("SAMEA1")
            .build();
    List<Sample> samples = Collections.singletonList(sample);
    String token = "Bearer token";

    AuthToken authToken =
        new AuthToken(
            "RS256", AuthorizationProvider.WEBIN, WEBIN_TESTING_ACCOUNT, Collections.emptyList());
    when(accessControlService.extractToken(any())).thenReturn(Optional.of(authToken));
    when(bioSamplesWebinAuthenticationService.buildSampleWithWebinId(any(), anyString()))
        .thenReturn(sample);
    when(sampleService.buildPrivateSample(any(Sample.class))).thenReturn(sample);
    when(sampleService.accessionSample(any(Sample.class))).thenReturn(accessionedSample);

    // Serialize samples to JSON
    String content = objectMapper.writeValueAsString(samples);

    mockMvc
        .perform(
            post("/samples/bulk-accession")
                .contentType(MediaType.APPLICATION_JSON)
                .content(content)
                .header("Authorization", token))
        .andExpect(status().isOk());
  }

  @Test
  public void testAccessionV2_WebinTokenInvalidException() throws Exception {
    // Arrange
    List<Sample> samples = Collections.singletonList(new Sample.Builder("test_1").build());
    String token = "Bearer invalid_token";

    when(accessControlService.extractToken(anyString())).thenReturn(Optional.empty());

    // Act & Assert
    mockMvc
        .perform(
            post("/samples/bulk-accession")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(samples))
                .header("Authorization", token))
        .andExpect(status().isUnauthorized())
        .andExpect(
            result ->
                assertTrue(
                    result.getResolvedException()
                        instanceof GlobalExceptions.WebinTokenInvalidException));
  }
}
