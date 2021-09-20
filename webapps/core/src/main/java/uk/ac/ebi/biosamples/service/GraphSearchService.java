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
package uk.ac.ebi.biosamples.service;

import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.neo4j.model.GraphSearchQuery;
import uk.ac.ebi.biosamples.neo4j.repo.NeoSampleRepository;

@Service
public class GraphSearchService {
  private static final Logger LOG = LoggerFactory.getLogger(GraphSearchService.class);

  private NeoSampleRepository neoSampleRepository;
  private SampleService sampleService;

  public GraphSearchService(NeoSampleRepository neoSampleRepository, SampleService sampleService) {
    this.neoSampleRepository = neoSampleRepository;
    this.sampleService = sampleService;
  }

  public List<Map<String, Object>> executeCypher(String query) {
    return neoSampleRepository.executeCypher(query);
  }

  public GraphSearchQuery graphSearch(GraphSearchQuery searchQuery, int size, int page) {
    return neoSampleRepository.graphSearch(searchQuery, size, page);
  }
}
