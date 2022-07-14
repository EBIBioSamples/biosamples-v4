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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import org.dom4j.DocumentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.hateoas.Resource;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.ega.EgaSampleExporter;
import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.structured.StructuredDataTable;

public class EnaCallable implements Callable<Void> {
  private final Logger log = LoggerFactory.getLogger(getClass());
  private static final String SUPPRESSED = "suppressed";
  private static final String TEMPORARY_SUPPRESSED = "temporary_suppressed";
  private static final String KILLED = "killed";
  private static final String TEMPORARY_KILLED = "temporary_killed";
  private final String accession;
  private final String egaId;
  private final BioSamplesClient bioSamplesWebinClient;
  private final EgaSampleExporter egaSampleExporter;
  private final EnaSampleTransformationService enaSampleTransformationService;
  private final Set<StructuredDataTable> amrData;

  private boolean suppressionHandler;
  private boolean killedHandler;
  private int statusId;

  public EnaCallable(
      String accession,
      String egaId,
      int statusId,
      BioSamplesClient bioSamplesWebinClient,
      EgaSampleExporter egaSampleExporter,
      EnaSampleTransformationService enaSampleTransformationService,
      boolean suppressionHandler,
      boolean killedHandler,
      Set<StructuredDataTable> amrData) {
    this.accession = accession;
    this.egaId = egaId;
    this.statusId = statusId;
    this.bioSamplesWebinClient = bioSamplesWebinClient;
    this.egaSampleExporter = egaSampleExporter;
    this.enaSampleTransformationService = enaSampleTransformationService;
    this.suppressionHandler = suppressionHandler;
    this.killedHandler = killedHandler;
    this.amrData = amrData;
  }

  @Override
  public Void call() throws Exception {
    if (egaId != null && !egaId.isEmpty()) {
      return egaSampleExporter.populateAndSubmitEgaData(egaId);
    } else if (suppressionHandler) {
      return checkAndUpdateSuppressedSample();
    } else if (killedHandler) {
      return checkAndUpdateKilledSamples();
    } else {
      try {
        final Sample sample = enaSampleTransformationService.enrichSample(this.accession, false);

        bioSamplesWebinClient.persistSampleResource(sample);
      } catch (final Exception e) {
        log.info("Failed to handle ENA sample with accession " + this.accession, e);
      }

      return null;
    }
  }

  /**
   * Checks samples from ENA which is SUPPRESSED and takes necessary action, i.e. update status if
   * status is different in BioSamples, else persist
   *
   * @return {@link Void}
   */
  private Void checkAndUpdateSuppressedSample() throws DocumentException {
    final List<String> curationDomainBlankList = new ArrayList<>();
    curationDomainBlankList.add("");

    try {
      Optional<Resource<Sample>> optionalSampleResource =
          bioSamplesWebinClient.fetchSampleResource(
              this.accession, Optional.of(curationDomainBlankList));

      if (optionalSampleResource.isPresent()) {
        final Sample sample = optionalSampleResource.get().getContent();
        boolean persistRequired = true;

        for (Attribute attribute : sample.getAttributes()) {
          if (attribute.getType().equals("INSDC status") && attribute.getValue().equals(SUPPRESSED)
              || attribute.getValue().equalsIgnoreCase(TEMPORARY_SUPPRESSED)) {
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
          log.info("Updating status to suppressed of sample: " + this.accession);
          bioSamplesWebinClient.persistSampleResource(sample);
        }
      }
    } catch (final RuntimeException e) {
      if (e.getMessage().contains("404")) {
        log.info("Accession doesn't exist " + this.accession + " creating the same");
        enaSampleTransformationService.enrichSample(this.accession, false);
      } else {
        log.error("Failed to update status of ENA sample " + accession + " to SUPPRESSED");
      }
    }

    return null;
  }

  /**
   * Checks samples from ENA which is KILLED and takes necessary action, i.e. update status if
   * status is different in BioSamples, else persist
   *
   * @return {@link Void}
   */
  private Void checkAndUpdateKilledSamples() throws DocumentException {
    final List<String> curationDomainBlankList = new ArrayList<>();
    curationDomainBlankList.add("");

    try {
      Optional<Resource<Sample>> optionalSampleResource =
          bioSamplesWebinClient.fetchSampleResource(
              this.accession, Optional.of(curationDomainBlankList));

      if (optionalSampleResource.isPresent()) {
        final Sample sample = optionalSampleResource.get().getContent();
        boolean persistRequired = true;

        for (Attribute attribute : sample.getAttributes()) {
          if (attribute.getType().equals("INSDC status")
              && (attribute.getValue().equals(KILLED)
                  || attribute.getValue().equals(TEMPORARY_KILLED))) {
            persistRequired = false;
            break;
          }
        }

        if (persistRequired) {
          sample.getAttributes().removeIf(attr -> attr.getType().contains("INSDC status"));
          sample
              .getAttributes()
              .add(Attribute.build("INSDC status", statusId == 6 ? KILLED : TEMPORARY_KILLED));
          log.info("Updating status to killed of sample: " + this.accession);
          bioSamplesWebinClient.persistSampleResource(sample);
        }
      }
    } catch (final Exception e) {
      if (e.getMessage().contains("404")) {
        log.info("Accession doesn't exist " + this.accession + " creating the same");
        enaSampleTransformationService.enrichSample(this.accession, false);
      } else {
        log.error("Failed to update status of ENA sample " + accession + " to KILLED");
      }
    }

    return null;
  }
}
