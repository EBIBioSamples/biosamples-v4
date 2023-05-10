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

import java.util.Collections;
import java.util.concurrent.Callable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.Curation;
import uk.ac.ebi.biosamples.model.ExternalReference;
import uk.ac.ebi.biosamples.model.Sample;

public class NcbiCallable implements Callable<Void> {
  private static final int MAX_RETRIES = 5;

  private final Logger log = LoggerFactory.getLogger(getClass());

  private final String accession;
  private final BioSamplesClient bioSamplesClient;
  private final String domain;

  private final EnaSampleToBioSampleConversionService enaSampleToBioSampleConversionService;

  /** Construction */
  NcbiCallable(
      final String accession,
      final BioSamplesClient bioSamplesClient,
      final String domain,
      final EnaSampleToBioSampleConversionService enaSampleToBioSampleConversionService) {
    this.accession = accession;
    this.bioSamplesClient = bioSamplesClient;
    this.domain = domain;
    this.enaSampleToBioSampleConversionService = enaSampleToBioSampleConversionService;
  }

  @Override
  public Void call() {
    boolean success = false;
    int numRetry = 0;

    try {
      // get the sample to make sure it exists first
      while (!success) {
        if (!bioSamplesClient.fetchSampleResource(accession).isPresent()) {
          log.info(
              "NCBI sample doesn't exists in BioSamples " + accession + " fetching from ERAPRO");

          final Sample sample =
              enaSampleToBioSampleConversionService.enrichSample(accession, true, null);

          try {
            bioSamplesClient.persistSampleResource(sample);

            success = true;
          } catch (final Exception e) {
            if (++numRetry == MAX_RETRIES) {
              throw new RuntimeException(
                  "Failed to enrich and persist NCBI sample with accession " + accession);
            }
          }
        } else {
          log.info("NCBI sample exists " + accession + " adding ENA link");

          final ExternalReference exRef =
              ExternalReference.build("https://www.ebi.ac.uk/ena/browser/view/" + accession);
          final Curation curation = Curation.build(null, null, null, Collections.singleton(exRef));

          try {
            bioSamplesClient.persistCuration(accession, curation, domain, false);
          } catch (final Exception e) {
            log.info("Failed to curate NCBI sample with ENA link " + accession);
          }

          success = true;
        }
      }
    } catch (final Exception e) {
      log.info("Failed to handle NCBI sample with accession " + accession, e);
    }

    return null;
  }
}
