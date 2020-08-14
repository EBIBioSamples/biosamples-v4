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
package uk.ac.ebi.biosamples.legacy.json.service;

import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import uk.ac.ebi.biosamples.legacy.json.domain.SamplesRelations;
import uk.ac.ebi.biosamples.legacy.json.domain.TestSample;
import uk.ac.ebi.biosamples.model.Sample;

// @RunWith(SpringRunner.class)
// @SpringBootTest
public class TestUtilities {

  @Autowired private static PagedResourcesAssembler<Sample> samplePagedResourcesAssembler;

  @Autowired
  private static PagedResourcesAssembler<SamplesRelations> sampleRelationsPagedResourcesAssembler;

  public static PagedResources<Resource<Sample>> getTestSamplePagedResources(
      int perPage, int totalSamples) {
    List<Sample> allSamples = new ArrayList<Sample>();
    for (int i = 0; i < perPage; i++) {
      allSamples.add(new TestSample(Integer.toString(i)).build());
    }

    Pageable pageInfo = new PageRequest(0, totalSamples);
    Page<Sample> samplePage = new PageImpl<>(allSamples, pageInfo, totalSamples);
    return samplePagedResourcesAssembler.toResource(samplePage);
  }

  public static PagedResources<Resource<SamplesRelations>> getTestSampleRelationsPagedResources(
      int perPage, int totalSamples) {
    List<SamplesRelations> allSamples = new ArrayList<>();
    for (int i = 0; i < perPage; i++) {
      allSamples.add(new SamplesRelations(new TestSample(Integer.toString(i)).build()));
    }

    Pageable pageInfo = new PageRequest(0, totalSamples);
    Page<SamplesRelations> samplePage = new PageImpl<>(allSamples, pageInfo, totalSamples);
    return sampleRelationsPagedResourcesAssembler.toResource(samplePage);
  }
}
