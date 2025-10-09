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

import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.hateoas.EntityModel;
import org.springframework.stereotype.Component;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.core.model.*;
import uk.ac.ebi.biosamples.neo4j.model.GraphNode;
import uk.ac.ebi.biosamples.neo4j.model.GraphSearchQuery;
import uk.ac.ebi.biosamples.neo4j.model.NeoSample;
import uk.ac.ebi.biosamples.neo4j.repo.NeoSampleRepository;
import uk.ac.ebi.biosamples.utils.IntegrationTestFailException;

@Component
public class SamplesGraphIntegration extends AbstractIntegration {
  private final Logger log = LoggerFactory.getLogger(getClass());
  private final NeoSampleRepository neoSampleRepository;

  public SamplesGraphIntegration(
      final BioSamplesClient client, final NeoSampleRepository neoSampleRepository) {
    super(client);
    this.neoSampleRepository = neoSampleRepository;
  }

  @Override
  protected void phaseOne() {
    // nothing to test here
  }

  @Override
  protected void phaseTwo() {
    // nothing to test here
  }

  @Override
  protected void phaseThree() {
    // nothing to test here
  }

  @Override
  protected void phaseFour() {
    final List<Sample> samples = new ArrayList<>();
    for (final EntityModel<Sample> sample : webinClient.fetchSampleResourceAll()) {
      samples.add(sample.getContent());
    }

    log.info("Sending {} samples to Neo4j", samples.size());
    for (final Sample sample : samples) {
      final NeoSample neoSample = NeoSample.build(sample);
      neoSampleRepository.loadSample(neoSample);
    }
  }

  @Override
  protected void phaseFive() {
    final GraphSearchQuery query = new GraphSearchQuery();
    final GraphNode node = new GraphNode();
    node.setId("a1");
    node.setType("Sample");

    final Map<String, String> attributes = new HashMap<>();
    attributes.put("organism", "homo sapiens");
    node.setAttributes(attributes);

    final Set<GraphNode> nodes = new HashSet<>();
    nodes.add(node);
    query.setNodes(nodes);
    query.setLinks(Collections.emptySet());

    final GraphSearchQuery response = neoSampleRepository.graphSearch(query, 10, 1);
    if (response.getNodes().isEmpty()) {
      throw new IntegrationTestFailException("No samples present in neo4j", Phase.FIVE);
    }
  }

  @Override
  protected void phaseSix() {}
}
