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
package uk.ac.ebi.biosamples.zooma;

import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.biosamples.PipelineResult;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.Curation;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.service.CurationApplicationService;

public class SampleZoomaCallable implements Callable<PipelineResult> {
  private final Logger log = LoggerFactory.getLogger(getClass());
  private final Sample sample;
  private final BioSamplesClient bioSamplesClient;
  private final ZoomaProcessor zoomaProcessor;
  private final CurationApplicationService curationApplicationService;
  private final String webinId;
  private int curationCount;
  static final ConcurrentLinkedQueue<String> failedQueue = new ConcurrentLinkedQueue<>();

  SampleZoomaCallable(
      final BioSamplesClient bioSamplesClient,
      final Sample sample,
      final ZoomaProcessor zoomaProcessor,
      final CurationApplicationService curationApplicationService,
      final String webinId) {
    this.bioSamplesClient = bioSamplesClient;
    this.sample = sample;
    this.zoomaProcessor = zoomaProcessor;
    this.curationApplicationService = curationApplicationService;
    this.webinId = webinId;
    curationCount = 0;
  }

  @Override
  public PipelineResult call() {
    boolean success = true;
    try {
      Sample last;
      Sample curated = sample;

      do {
        last = curated;
        curated = zooma(last);
      } while (!last.equals(curated));
    } catch (final Exception e) {
      log.warn("Encountered exception with " + sample.getAccession(), e);
      failedQueue.add(sample.getAccession());
      success = false;
    }

    return new PipelineResult(sample.getAccession(), curationCount, success);
  }

  private Sample zooma(Sample sample) {
    for (final Attribute attribute : sample.getAttributes()) {
      // if there are any iris already, skip zoomafying it and curate elsewhere
      if (attribute.getIri().size() > 0) {
        continue;
      }

      // do nothing - removed a loop as attribute.getIri() is always null

      // if it has units, skip it
      if (attribute.getUnit() != null) {
        continue;
      }

      if (attribute.getType().equalsIgnoreCase("synonym")) {
        log.trace("Skipping synonym " + attribute.getValue());
        continue;
      }

      if (attribute.getType().equalsIgnoreCase("other")) {
        log.trace("Skipping other " + attribute.getValue());
        continue;
      }

      if (attribute.getType().equalsIgnoreCase("unknown")) {
        log.trace("Skipping unknown " + attribute.getValue());
        continue;
      }

      if (attribute.getType().equalsIgnoreCase("description")) {
        log.trace("Skipping description " + attribute.getValue());
        continue;
      }

      if (attribute.getType().equalsIgnoreCase("label")) {
        log.trace("Skipping label " + attribute.getValue());
        continue;
      }

      if ("model".equalsIgnoreCase(attribute.getType())
          || "package".equalsIgnoreCase(attribute.getType())
          || "INSDC first public".equals(attribute.getType())
          || "INSDC last update".equals(attribute.getType())
          || "NCBI submission model".equals(attribute.getType())
          || "NCBI submission package".equals(attribute.getType())
          || "INSDC status".equals(attribute.getType())
          || "ENA checklist".equals(attribute.getType())
          || "INSDC center name".equals(attribute.getType())) {
        log.trace("Skipping " + attribute.getType() + " : " + attribute.getValue());
        continue;
      }

      if (attribute.getType().equalsIgnoreCase("host_subject_id")) {
        log.trace("Skipping host_subject_id " + attribute.getValue());
        continue;
      }

      if (attribute.getValue().matches("^[0-9.-]+$")) {
        log.trace("Skipping number " + attribute.getValue());
        continue;
      }

      if (attribute.getValue().matches("^[ESD]R[SRX][0-9]+$")) {
        log.trace("Skipping SRA/ENA/DDBJ identifier " + attribute.getValue());
        continue;
      }

      if (attribute.getValue().matches("^GSM[0-9]+$")) {
        log.trace("Skipping GEO identifier " + attribute.getValue());
        continue;
      }

      if (attribute.getValue().matches("^SAM[END]A?G?[0-9]+$")) {
        log.trace("Skipping BioSample identifier " + attribute.getValue());
        continue;
      }

      if (attribute.getType().length() < 64 && attribute.getValue().length() < 128) {
        final Optional<String> iri =
            zoomaProcessor.queryZooma(attribute.getType(), attribute.getValue());

        if (iri.isPresent()) {
          log.trace("Mapped " + attribute + " to " + iri.get());
          final Attribute mapped =
              Attribute.build(
                  attribute.getType(), attribute.getValue(), attribute.getTag(), iri.get(), null);
          final Curation curation =
              Curation.build(
                  Collections.singleton(attribute), Collections.singleton(mapped), null, null);

          // save the curation back in biosamples
          bioSamplesClient.persistCuration(sample.getAccession(), curation, webinId);
          sample = curationApplicationService.applyCurationToSample(sample, curation);
          curationCount++;
        }
      }
    }

    return sample;
  }
}
