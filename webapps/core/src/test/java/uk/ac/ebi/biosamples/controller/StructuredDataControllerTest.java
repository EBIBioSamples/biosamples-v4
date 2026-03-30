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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Collections;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import uk.ac.ebi.biosamples.core.model.structured.StructuredData;
import uk.ac.ebi.biosamples.core.model.structured.StructuredDataTable;
import uk.ac.ebi.biosamples.exception.GlobalExceptions;
import uk.ac.ebi.biosamples.security.TestSecurityConfig;
import uk.ac.ebi.biosamples.service.SampleService;
import uk.ac.ebi.biosamples.service.StructuredDataService;
import uk.ac.ebi.biosamples.service.WebinAuthenticationService;

@RunWith(SpringRunner.class)
@SpringBootTest(properties = {"spring.cloud.gcp.project-id=no_project"})
@ContextConfiguration(classes = TestSecurityConfig.class)
public class StructuredDataControllerTest {
  private static final String WEBIN_TESTING_ACCOUNT = "Webin-12345";

  @Autowired private WebApplicationContext context;
  @MockBean private WebinAuthenticationService webinAuthenticationService;
  @MockBean private StructuredDataService structuredDataService;
  @MockBean private SampleService sampleService;

  private MockMvc mockMvc;
  private ObjectMapper objectMapper;

  @Before
  public void setUp() {
    objectMapper = new ObjectMapper();
    mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
  }

  @Test
  public void getStructuredData_whenFound_returnsOk() throws Exception {
    final String accession = "SAMEA123";
    final StructuredData sd =
        StructuredData.build(
            accession, Instant.parse("2026-03-18T12:00:00Z"), Collections.emptySet());

    when(structuredDataService.getStructuredData(accession)).thenReturn(Optional.of(sd));

    mockMvc
        .perform(get("/structureddata/{accession}", accession).accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accession").value(accession))
        .andExpect(jsonPath("$.data").exists())
        .andExpect(jsonPath("$.structuredData").doesNotExist());
  }

  @Test
  public void putStructuredData_whenUnauthenticated_isRejected() throws Exception {
    final String accession = "SAMEA123";
    final String requestJson =
        "{"
            + "\"accession\":\""
            + accession
            + "\","
            + "\"create\":\"2026-03-18T12:00:00Z\","
            + "\"update\":\"2026-03-18T12:00:00Z\","
            + "\"data\":[{\"domain\":null,\"webinSubmissionAccountId\":\""
            + WEBIN_TESTING_ACCOUNT
            + "\",\"type\":\"AMR\",\"schema\":null,\"content\":[]}]"
            + "}";

    mockMvc
        .perform(
            put("/structureddata/{accession}", accession)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaTypes.HAL_JSON)
                .content(requestJson))
        .andExpect(status().is4xxClientError());
  }

  @Test
  @WithUserDetails(WEBIN_TESTING_ACCOUNT)
  public void putStructuredData_whenAuthenticated_persistsAndReturnsBody() throws Exception {
    final String accession = "SAMEA123";
    final StructuredDataTable table =
        StructuredDataTable.build(null, WEBIN_TESTING_ACCOUNT, "AMR", null, Collections.emptySet());
    final StructuredData request =
        StructuredData.build(
            accession, Instant.parse("2026-03-18T12:00:00Z"), Collections.singleton(table));

    when(sampleService.getPrinciple(any())).thenReturn(WEBIN_TESTING_ACCOUNT);
    doNothing().when(webinAuthenticationService).isStructuredDataAccessible(eq(request), any());
    when(structuredDataService.saveStructuredData(eq(request))).thenReturn(request);

    mockMvc
        .perform(
            put("/structureddata/{accession}", accession)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accession").value(accession))
        .andExpect(jsonPath("$.data[0].type").value("AMR"))
        .andExpect(jsonPath("$.structuredData").doesNotExist());

    verify(sampleService).getPrinciple(any());
    verify(webinAuthenticationService)
        .isStructuredDataAccessible(eq(request), eq(WEBIN_TESTING_ACCOUNT));
    verify(structuredDataService).saveStructuredData(eq(request));
  }

  @Test
  @WithUserDetails(WEBIN_TESTING_ACCOUNT)
  public void putStructuredData_whenUsingStructuredDataField_isRejected() throws Exception {
    final String accession = "SAMEA123";
    final String requestJson =
        "{"
            + "\"accession\":\""
            + accession
            + "\","
            + "\"create\":\"2026-03-18T12:00:00Z\","
            + "\"update\":\"2026-03-18T12:00:00Z\","
            + "\"structuredData\":[{\"domain\":null,\"webinSubmissionAccountId\":\""
            + WEBIN_TESTING_ACCOUNT
            + "\",\"type\":\"AMR\",\"schema\":null,\"content\":[]}]"
            + "}";

    when(sampleService.getPrinciple(any())).thenReturn(WEBIN_TESTING_ACCOUNT);
    when(structuredDataService.saveStructuredData(any()))
        .thenThrow(new GlobalExceptions.SampleMandatoryFieldsMissingException("Missing data"));

    mockMvc
        .perform(
            put("/structureddata/{accession}", accession)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(requestJson))
        .andExpect(status().isBadRequest());
  }
}
