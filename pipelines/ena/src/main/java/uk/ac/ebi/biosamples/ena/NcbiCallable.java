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

import java.util.*;
import java.util.concurrent.Callable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.hateoas.Resource;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.*;

public class NcbiCallable implements Callable<Void> {
  private Logger log = LoggerFactory.getLogger(getClass());

  private static final String SUPPRESSED = "suppressed";
  public static final String TEMPORARY_SUPPRESSED = "temporary_suppressed";

  private final String accession;
  private final int statusId;
  private final BioSamplesClient bioSamplesClient;
  private final String domain;
  private final boolean suppressionHandler;

  private final EnaSampleTransformationService enaSampleTransformationService;

  /** Construction */
  public NcbiCallable(
      String accession,
      int statusId,
      BioSamplesClient bioSamplesClient,
      String domain,
      boolean suppressionHandler,
      EnaSampleTransformationService enaSampleTransformationService) {
    this.accession = accession;
    this.statusId = statusId;
    this.bioSamplesClient = bioSamplesClient;
    this.domain = domain;
    this.suppressionHandler = suppressionHandler;
    this.enaSampleTransformationService = enaSampleTransformationService;
  }

  @Override
  public Void call() {
    if (suppressionHandler) {
      checkAndUpdateSuppressedSample();
    } else {
      // get the sample to make sure it exists first
      if (!bioSamplesClient.fetchSampleResource(this.accession).isPresent()) {
        log.info(
            "NCBI sample doesn't exists in BioSamples " + this.accession + " fetching from ERAPRO");

        try {
          final Sample sample = enaSampleTransformationService.enrichSample(this.accession, true);

          bioSamplesClient.persistSampleResource(sample);
        } catch (final Exception e) {
          e.printStackTrace();

          log.info("Failed to handle NCBI sample with accession " + this.accession);
        }
      } else {
        log.info("NCBI sample exists " + this.accession + " adding ENA link");

        ExternalReference exRef =
            ExternalReference.build("https://www.ebi.ac.uk/ena/browser/view/" + this.accession);
        Curation curation = Curation.build(null, null, null, Collections.singleton(exRef));

        try {
          bioSamplesClient.persistCuration(this.accession, curation, domain, false);
        } catch (final Exception e) {
          log.info("Failed to curate NCBI sample with ENA link " + this.accession);
        }
      }
    }

    return null;
  }

  /**
   * Checks if sample status is not SUPPRESSED in BioSamples, if yes then persists the sample with
   * SUPPRESSED status
   */
  private void checkAndUpdateSuppressedSample() {
    final List<String> curationDomainBlankList = new ArrayList<>();
    curationDomainBlankList.add("");

    try {
      final Optional<Resource<Sample>> optionalSampleResource =
          bioSamplesClient.fetchSampleResource(
              this.accession, Optional.of(curationDomainBlankList));

      if (optionalSampleResource.isPresent()) {
        final Sample sample = optionalSampleResource.get().getContent();
        boolean persistRequired = true;

        for (Attribute attribute : sample.getAttributes()) {
          if (attribute.getType().equals("INSDC status")
              && (attribute.getValue().equals(SUPPRESSED)
                  || attribute.getValue().equalsIgnoreCase(TEMPORARY_SUPPRESSED))) {
            persistRequired = false;
            break;
          }
        }

        if (persistRequired) {
          sample.getAttributes().removeIf(attr -> attr.getType().contains("INSDC status"));
          sample
              .getAttributes()
              .add(
                  Attribute.build(
                      "INSDC status", statusId == 5 ? SUPPRESSED : TEMPORARY_SUPPRESSED));
          log.info("Updating status to suppressed/ temp-suppressed of sample: " + this.accession);
          bioSamplesClient.persistSampleResource(sample);
        }
      }
    } catch (final Exception e) {
      log.error("Failed to update sample status to suppressed for NCBI sample " + accession);
    }
  }
}
