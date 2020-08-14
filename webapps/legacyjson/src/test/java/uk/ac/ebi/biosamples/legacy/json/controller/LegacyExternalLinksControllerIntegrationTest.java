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
package uk.ac.ebi.biosamples.legacy.json.controller;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasKey;
import static org.springframework.hateoas.MediaTypes.HAL_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import uk.ac.ebi.biosamples.legacy.json.repository.SampleRepository;

@RunWith(SpringRunner.class)
@AutoConfigureMockMvc
@SpringBootTest
public class LegacyExternalLinksControllerIntegrationTest {

  @MockBean private SampleRepository sampleRepositoryMock;

  @Autowired private MockMvc mockMvc;

  @Test
  public void testExternalLinksIndexReturnPagedResourcesOfExternalLinks() throws Exception {
    mockMvc
        .perform(get("/externallinksrelations").accept(HAL_JSON))
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/hal+json;charset=UTF-8"))
        .andExpect(
            jsonPath("$").value(allOf(hasKey("_embedded"), hasKey("_links"), hasKey("page"))));
  }

  @Test
  @Ignore
  public void testReturnExternalLinkByLinkName() throws Exception {
    /*TODO */
  }

  @Test
  @Ignore
  public void textExternalLinksContainsExpectedLinks() throws Exception {
    /* TODO */
  }

  @Test
  public void testExternalLinksIndexContainsSearchLink() throws Exception {
    mockMvc
        .perform(get("/externallinksrelations").accept(HAL_JSON))
        .andExpect(jsonPath("$._links").value(hasKey("search")));
  }

  @Test
  @Ignore
  public void testExternalLinksSearchContainsFindOneByUrlLink() throws Exception {
    /*TODO */
  }

  @Test
  @Ignore
  public void testFindOneByUrlSearchReturnExternalLink() throws Exception {
    /*TODO */
  }

  @Test
  @Ignore
  public void testOrganizationIsRootField() throws Exception {
    /*TODO */
  }
}
