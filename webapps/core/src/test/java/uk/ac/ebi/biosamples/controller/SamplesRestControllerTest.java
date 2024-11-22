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

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import uk.ac.ebi.biosamples.BioSamplesProperties;
import uk.ac.ebi.biosamples.docs.DocumentationHelper;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.mongo.service.CurationReadService;
import uk.ac.ebi.biosamples.mongo.service.SampleReadService;
import uk.ac.ebi.biosamples.security.TestSecurityConfig;
import uk.ac.ebi.biosamples.service.*;
import uk.ac.ebi.biosamples.service.security.WebinAuthenticationService;
import uk.ac.ebi.biosamples.service.taxonomy.TaxonomyClientService;
import uk.ac.ebi.biosamples.solr.repo.CursorArrayList;
import uk.ac.ebi.biosamples.validation.SchemaValidationService;

@RunWith(SpringRunner.class)
@SpringBootTest(properties = {"spring.cloud.gcp.project-id=no_project"})
@ContextConfiguration(classes = TestSecurityConfig.class)
public class SamplesRestControllerTest {
  private static final String WEBIN_TESTING_ACCOUNT = "Webin-12345";
  @Autowired private WebApplicationContext context;
  @MockBean private SamplePageService samplePageService;
  @MockBean private FilterService filterService;
  @MockBean private WebinAuthenticationService webinAuthenticationService;
  @MockBean private SampleManipulationService sampleManipulationService;
  @MockBean private SampleService sampleService;
  @MockBean private BioSamplesProperties bioSamplesProperties;
  @MockBean private SampleResourceAssembler sampleResourceAssembler;
  @MockBean private SchemaValidationService schemaValidationService;
  @MockBean private TaxonomyClientService taxonomyClientService;
  @MockBean private SampleReadService sampleReadService;
  @MockBean private CurationPersistService curationPersistService;
  @MockBean private CurationReadService curationReadService;

  private DocumentationHelper faker;
  private MockMvc mockMvc;

  @Before
  public void setUp() {
    faker = new DocumentationHelper();
    mockMvc =
        MockMvcBuilders.webAppContextSetup(context)
            .defaultRequest(get("/").contextPath("/biosamples"))
            .build();
  }

  @Test
  @WithUserDetails(WEBIN_TESTING_ACCOUNT)
  @Ignore
  public void testPostSample() throws Exception {
    // Arrange
    Sample sample =
        new Sample.Builder("TEST_ACCESSION")
            .withWebinSubmissionAccountId(bioSamplesProperties.getBiosamplesClientWebinUsername())
            .build();

    // Simulate sample processing and response
    when(sampleService.getPrinciple(any())).thenReturn("test-user");
    when(webinAuthenticationService.isWebinSuperUser(anyString())).thenReturn(false);
    when(sampleService.buildPrivateSample(any())).thenReturn(sample);
    when(sampleService.persistSample(any(), any(), anyBoolean())).thenReturn(sample);
    when(sampleResourceAssembler.toModel(any(Sample.class))).thenReturn(EntityModel.of(sample));

    ObjectMapper objectMapper = new ObjectMapper();

    // Act & Assert
    mockMvc
        .perform(
            post("/biosamples/samples")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sample)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accession").value("TEST_ACCESSION"))
        .andExpect(jsonPath("$.name").value("Test Sample"))
        .andExpect(jsonPath("$.domain").value("test-domain"));

    verify(sampleService).getPrinciple(any());
    verify(sampleService).buildPrivateSample(any());
    verify(sampleService).persistSample(any(), any(), anyBoolean());
    verify(webinAuthenticationService).isWebinSuperUser(anyString());
  }

  @Test
  @Ignore
  public void getSamples() throws Exception {
    final Sample fakeSample = faker.getExampleSample();
    final CursorArrayList<Sample> sampleCursorArrayList =
        new CursorArrayList<>(Collections.singletonList(fakeSample), "");

    when(samplePageService.getSamplesByText(
            nullable(String.class),
            anyList(),
            nullable(String.class),
            nullable(String.class),
            anyInt(),
            any()))
        .thenReturn(sampleCursorArrayList);

    mockMvc
        .perform(get("/biosamples/samples").accept(MediaTypes.HAL_JSON))
        .andExpect(status().isOk());
  }

  @Test
  public void throwErrorForLargePageNumbers() throws Exception {
    mockMvc
        .perform(get("/biosamples/samples?page=502").accept(MediaTypes.HAL_JSON))
        .andExpect(status().isBadRequest());
  }
}
