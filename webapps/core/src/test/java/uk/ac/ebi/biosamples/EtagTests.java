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
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;
import java.util.Optional;
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
import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.AuthToken;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.auth.AuthorizationProvider;
import uk.ac.ebi.biosamples.service.SampleService;
import uk.ac.ebi.biosamples.service.security.AccessControlService;
import uk.ac.ebi.biosamples.service.security.BioSamplesAapService;

@RunWith(SpringRunner.class)
@SpringBootTest(properties = {"spring.cloud.gcp.project-id=no_project"})
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class EtagTests {

  @Autowired private MockMvc mockMvc;
  @MockBean private BioSamplesAapService bioSamplesAapService;
  @MockBean private SampleService sampleService;
  @MockBean private AccessControlService accessControlService;

  @Test
  public void get_validation_endpoint_return_not_allowed_response() throws Exception {
    final String sampleAccession = "SAMEA123456789";
    final Sample testSample =
        new Sample.Builder("TestSample", sampleAccession)
            .withDomain("TestDomain")
            .addAttribute(new Attribute.Builder("Organism", "Homo sapiens").build())
            .build();

    when(sampleService.fetch(sampleAccession, Optional.empty()))
        .thenReturn(Optional.of(testSample));
    when(bioSamplesAapService.handleSampleDomain(testSample, Optional.empty()))
        .thenReturn(testSample);
    doNothing().when(bioSamplesAapService).isSampleAccessible(isA(Sample.class));
    when(accessControlService.extractToken(anyString()))
        .thenReturn(
            Optional.of(
                new AuthToken(
                    "RS256", AuthorizationProvider.AAP, "user", Collections.emptyList())));

    mockMvc
        .perform(get("/samples/{accession}", sampleAccession).accept(MediaTypes.HAL_JSON))
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
