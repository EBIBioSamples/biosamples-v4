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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.security.TestSecurityConfig;
import uk.ac.ebi.biosamples.service.SampleService;
import uk.ac.ebi.biosamples.service.security.WebinAuthenticationService;

@SpringBootTest(properties = {"spring.cloud.gcp.project-id=no_project"})
@ContextConfiguration(classes = TestSecurityConfig.class)
@AutoConfigureMockMvc
public class BulkActionControllerV2Test {
  private static final String WEBIN_TESTING_ACCOUNT = "Webin-12345";
  @Autowired private MockMvc mockMvc;
  @MockBean private SampleService sampleService;
  @MockBean private WebinAuthenticationService webinAuthenticationService;
  @Autowired private ObjectMapper objectMapper;

  @BeforeEach
  public void setUp() {
    // No need for MockitoAnnotations.openMocks(this), as @MockBean takes care of mocking in the
    // Spring context.
  }

  @Test
  @WithUserDetails(WEBIN_TESTING_ACCOUNT)
  public void testGetV2_SuccessfulFetch() throws Exception {
    // Arrange
    Sample sample =
        new Sample.Builder("test_1")
            .withWebinSubmissionAccountId(WEBIN_TESTING_ACCOUNT)
            .withAccession("SAMEA1")
            .withRelease("2047-01-01T12:00:00")
            .build();
    when(sampleService.fetch(anyString(), anyBoolean())).thenReturn(Optional.of(sample));
    doCallRealMethod()
        .when(webinAuthenticationService)
        .isSampleAccessible(sample, WEBIN_TESTING_ACCOUNT);

    // Act & Assert
    mockMvc
        .perform(
            get("/samples/bulk-fetch")
                .param("accessions", "SAMEA1")
                .header("Authorization", "Bearer token"))
        .andExpect(status().isOk());
  }

  @Test
  @WithUserDetails(WEBIN_TESTING_ACCOUNT)
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
    String token = "header.payload.signature";

    when(webinAuthenticationService.buildSampleWithWebinId(any(), anyString())).thenReturn(sample);
    when(sampleService.getPrinciple(any(Authentication.class))).thenReturn(WEBIN_TESTING_ACCOUNT);
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
  @WithUserDetails(WEBIN_TESTING_ACCOUNT)
  public void testAccessionV2_SuccessfulAccession_2() throws Exception {
    // Arrange
    Sample sample =
        new Sample.Builder("test_1").withWebinSubmissionAccountId(WEBIN_TESTING_ACCOUNT).build();
    Sample accessionedSample =
        new Sample.Builder("test_1")
            .withWebinSubmissionAccountId(WEBIN_TESTING_ACCOUNT)
            .withAccession("SAMEA1")
            .build();
    List<Sample> samples = Collections.singletonList(sample);
    String token = "header.payload.signature";

    when(webinAuthenticationService.buildSampleWithWebinId(any(), anyString())).thenReturn(sample);
    when(sampleService.getPrinciple(any(Authentication.class))).thenReturn(WEBIN_TESTING_ACCOUNT);
    when(sampleService.buildPrivateSample(any(Sample.class))).thenReturn(sample);
    when(sampleService.accessionSample(any(Sample.class))).thenReturn(accessionedSample);

    // Act & Assert
    mockMvc
        .perform(
            post("/samples/bulk-accession")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(samples))
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk());
  }
}
