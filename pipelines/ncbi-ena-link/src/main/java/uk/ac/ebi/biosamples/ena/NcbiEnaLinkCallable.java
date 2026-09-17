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
package uk.ac.ebi.biosamples.ena;

import java.util.Optional;
import java.util.concurrent.Callable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.hateoas.EntityModel;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.core.model.Sample;
import uk.ac.ebi.biosamples.service.EnaSampleToBioSampleConversionService;

public class NcbiEnaLinkCallable implements Callable<Void> {
  private static final int MAX_RETRIES = 5; // Maximum number of retries for persistence operation
  private final Logger log = LoggerFactory.getLogger(getClass());
  private final String accession;
  private final BioSamplesClient bioSamplesClient;
  private final EnaSampleToBioSampleConversionService enaSampleToBioSampleConversionService;

  /** Constructor to initialize the callable with necessary dependencies */
  NcbiEnaLinkCallable(
      final String accession,
      final BioSamplesClient bioSamplesClient,
      final EnaSampleToBioSampleConversionService enaSampleToBioSampleConversionService) {
    this.accession = accession;
    this.bioSamplesClient = bioSamplesClient;
    this.enaSampleToBioSampleConversionService = enaSampleToBioSampleConversionService;
  }

  /**
   * The main task execution method which will be called when the task is executed. It retrieves the
   * sample, checks if it exists in BioSamples, and enriches and persists it if necessary.
   */
  @Override
  public Void call() {
    boolean success = false;

    try {
      // Retrieve the sample to check its existence
      Optional<EntityModel<Sample>> optionalSampleEntityModel =
          bioSamplesClient.fetchSampleResource(accession, false);

      if (optionalSampleEntityModel.isEmpty()) {
        log.info("NCBI sample {} doesn't exist in BioSamples, fetching from ERAPRO", accession);

        // Enrich the sample from ERA PRO
        final Sample sample = enaSampleToBioSampleConversionService.enrichSample(accession, true);

        // Attempt to persist the enriched sample with retries
        submitRetry(success, sample);
      }
    } catch (final Exception e) {
      NcbiEnaLinkRunner.failures.put(accession, e.getMessage());
      log.error("Failed to handle NCBI sample with accession {}", accession, e);
    }

    return null;
  }

  /**
   * Attempts to persist the modified sample, retrying if necessary.
   *
   * @param success - boolean indicating if the persistence was successful
   * @param modifiedSample - the sample to be persisted
   */
  private void submitRetry(boolean success, Sample modifiedSample) {
    int numRetry = 0;

    while (!success) {
      try {
        bioSamplesClient.persistSampleResource(modifiedSample);
        success = true;
      } catch (final Exception e) {
        if (++numRetry == MAX_RETRIES) {
          throw new RuntimeException(
              "Failed to enrich and persist NCBI sample with accession " + accession, e);
        }
      }
    }
  }
}
