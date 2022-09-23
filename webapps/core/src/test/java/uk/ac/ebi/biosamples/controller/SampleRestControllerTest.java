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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.hateoas.MediaTypes;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import uk.ac.ebi.biosamples.docs.DocumentationHelper;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.service.CurationPersistService;
import uk.ac.ebi.biosamples.service.SamplePageService;
import uk.ac.ebi.biosamples.solr.repo.CursorArrayList;
import uk.ac.ebi.biosamples.utils.mongo.CurationReadService;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@RunWith(SpringRunner.class)
@SpringBootTest
public class SampleRestControllerTest {

  @Autowired
  private WebApplicationContext context;

  @MockBean
  private SamplePageService samplePageService;

  @MockBean
  CurationPersistService curationPersistService;

  @MockBean
  CurationReadService curationReadService;

  private DocumentationHelper faker;
  private MockMvc mockMvc;

  @Before
  public void setUp() {
    faker = new DocumentationHelper();
    mockMvc =
        MockMvcBuilders.webAppContextSetup(this.context)
                       .defaultRequest(get("/").contextPath("/biosamples"))
                       .build();
  }

  @Test
  public void getSamples() throws Exception {
    Sample fakeSample = faker.getExampleSample();
    CursorArrayList<Sample> sampleCursorArrayList =
        new CursorArrayList<>(Collections.singletonList(fakeSample), "");
    when(samplePageService.getSamplesByText(nullable(String.class), anyList(), anySet(), nullable(String.class),
                                            nullable(String.class), anyInt(), any(), any()))
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
