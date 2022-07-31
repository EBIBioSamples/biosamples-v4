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
package uk.ac.ebi.biosamples.curation;

import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.hateoas.EntityModel;
import uk.ac.ebi.biosamples.PipelineResult;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.Relationship;
import uk.ac.ebi.biosamples.model.Sample;

public class TransformationCallable implements Callable<PipelineResult> {
  private static final Logger LOG = LoggerFactory.getLogger(TransformationCallable.class);
  static final ConcurrentLinkedQueue<String> failedQueue = new ConcurrentLinkedQueue<>();

  private final Sample sample;
  private final BioSamplesClient bioSamplesClient;

  public TransformationCallable(
      @Qualifier("WEBINCLIENT") BioSamplesClient bioSamplesClient, Sample sample) {
    this.bioSamplesClient = bioSamplesClient;
    this.sample = sample;
  }

  @Override
  public PipelineResult call() {
    int modifiedRecords = 0;

    Optional<EntityModel<Sample>> optionalSampleResource =
        bioSamplesClient.fetchSampleResource(sample.getAccession(), Optional.of(new ArrayList<>()));
    if (optionalSampleResource.isPresent()) {
      Sample uncuratedSample = optionalSampleResource.get().getContent();
      Optional<Attribute> optionalRelAttribute =
          uncuratedSample.getAttributes().stream()
              .filter(a -> a.getType().equalsIgnoreCase("sample derived from"))
              .findFirst();

      if (optionalRelAttribute.isPresent()) {
        Attribute attribute = optionalRelAttribute.get();
        if (uncuratedSample.getRelationships().size() == 2) {
          Relationship wrongRel = uncuratedSample.getRelationships().first();
          uncuratedSample.getRelationships().remove(wrongRel);
          LOG.info("Removed relationship of {}: {}", sample.getAccession(), wrongRel);
          wrongRel = uncuratedSample.getRelationships().first();
          uncuratedSample.getRelationships().remove(wrongRel);
          LOG.info("Removed relationship of {}: {}", sample.getAccession(), wrongRel);
        }

        uncuratedSample
            .getRelationships()
            .add(createDerivedRelationship(uncuratedSample.getAccession(), attribute.getValue()));
        bioSamplesClient.persistSampleResource(uncuratedSample);
        LOG.info(
            "Copied derived from relationship from attribute {} -> {}",
            uncuratedSample.getAccession(),
            attribute.getValue());
        modifiedRecords++;
      } else {
        LOG.info(
            "Attribute sample derived from is not present in ths sample : {}",
            sample.getAccession());
      }
    }

    return new PipelineResult(sample.getAccession(), modifiedRecords, true);
  }

  private Relationship createDerivedRelationship(String source, String target) {
    return Relationship.build(source, "derived from", target);
  }
}
