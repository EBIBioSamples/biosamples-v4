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

import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.hateoas.EntityModel;
import uk.ac.ebi.biosamples.PipelineResult;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.core.model.Attribute;
import uk.ac.ebi.biosamples.core.model.Relationship;
import uk.ac.ebi.biosamples.core.model.Sample;

public class TransformationCallable implements Callable<PipelineResult> {
  private static final Logger LOG = LoggerFactory.getLogger(TransformationCallable.class);
  static final ConcurrentLinkedQueue<String> failedQueue = new ConcurrentLinkedQueue<>();
  private final Sample sample;
  private final BioSamplesClient bioSamplesClientWebin;

  TransformationCallable(final Sample sample, final BioSamplesClient bioSamplesClientWebin) {
    this.sample = sample;
    this.bioSamplesClientWebin = bioSamplesClientWebin;
  }

  @Override
  public PipelineResult call() {
    int modifiedRecords = 0;

    final Optional<EntityModel<Sample>> optionalSampleResource =
        bioSamplesClientWebin.fetchSampleResource(sample.getAccession(), false);

    if (optionalSampleResource.isPresent()) {
      final Sample uncuratedSample = optionalSampleResource.get().getContent();
      final Optional<Attribute> optionalRelAttribute =
          uncuratedSample.getAttributes().stream()
              .filter(a -> a.getType().equalsIgnoreCase("sample derived from"))
              .findFirst();

      if (optionalRelAttribute.isPresent()) {
        final Attribute attribute = optionalRelAttribute.get();

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

        try {
          LOG.info(
              "Copying derived from relationship from attribute {} -> {}",
              uncuratedSample.getAccession(),
              attribute.getValue());

          final Sample persistedSample = persistSample(uncuratedSample);

          LOG.debug("Sample persisted with relationships: {}", persistedSample.getAccession());

          modifiedRecords++;
        } catch (final Exception e) {
          LOG.error("Failed to persist sample: {}", sample.getAccession(), e);
          LOG.warn(
              "Ignoring failed sample and processing rest of the records. Future work may required to process failed record.");
        }
      } else {
        LOG.info(
            "Attribute sample derived from is not present in ths sample : {}",
            sample.getAccession());
      }
    }

    return new PipelineResult(sample.getAccession(), modifiedRecords, true);
  }

  private Sample persistSample(final Sample s) {
    return bioSamplesClientWebin.persistSampleResource(s).getContent();
  }

  private Relationship createDerivedRelationship(final String source, final String target) {
    return Relationship.build(source, "derived from", target);
  }
}
