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
import java.util.Set;
import java.util.concurrent.Callable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.hateoas.EntityModel;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.ega.EgaSampleExporter;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.SubmittedViaType;
import uk.ac.ebi.biosamples.model.structured.AbstractData;
import uk.ac.ebi.biosamples.model.structured.StructuredDataTable;

public class EnaImportCallable implements Callable<Void> {
  private final Logger log = LoggerFactory.getLogger(getClass());
  private static final int MAX_RETRIES = 5;
  private static final String ENA_CHECKLIST = "ENA-CHECKLIST";
  private final String accession;
  private final String egaId;
  private final BioSamplesClient bioSamplesWebinClient;
  private final EgaSampleExporter egaSampleExporter;
  private final EnaSampleToBioSampleConversionService enaSampleToBioSampleConversionService;

  EnaImportCallable(
      final String accession,
      final String egaId,
      final BioSamplesClient bioSamplesWebinClient,
      final EgaSampleExporter egaSampleExporter,
      final EnaSampleToBioSampleConversionService enaSampleToBioSampleConversionService) {
    this.accession = accession;
    this.egaId = egaId;
    this.bioSamplesWebinClient = bioSamplesWebinClient;
    this.egaSampleExporter = egaSampleExporter;
    this.enaSampleToBioSampleConversionService = enaSampleToBioSampleConversionService;
  }

  @Override
  public Void call() {
    Sample sample;

    if (egaId != null && !egaId.isEmpty()) {
      return egaSampleExporter.populateAndSubmitEgaData(egaId);
    } else {
      try {
        final Optional<EntityModel<Sample>> sampleOptionalInBioSamples =
            bioSamplesWebinClient.fetchSampleResource(accession);
        final Sample sampleInBioSamples =
            sampleOptionalInBioSamples.map(EntityModel::getContent).orElse(null);
        Set<AbstractData> oldStructuredData = null;
        Set<StructuredDataTable> newStructuredData = null;

        boolean isSampleImportFromEraDbRequired = true;

        if (sampleInBioSamples != null) {
          oldStructuredData = sampleInBioSamples.getData();
          newStructuredData = sampleInBioSamples.getStructuredData();

          if (sampleInBioSamples.getSubmittedVia() == SubmittedViaType.FILE_UPLOADER) {
            log.info(
                "ENA sample has been updated in BioSamples using the FILE Uploader, don't re-import "
                    + accession);

            isSampleImportFromEraDbRequired = false;
          } /*else if (sampleInBioSamples.getAttributes().stream()
                .anyMatch(attribute -> attribute.getType().equalsIgnoreCase(ENA_CHECKLIST))) {
              log.info(
                  "ENA sample already exists with attributes in BioSamples, don't re-import  "
                      + accession);

              isSampleImportFromEraDbRequired = false;
            }*/
        }

        boolean success = false;
        int numRetry = 0;

        if (isSampleImportFromEraDbRequired) {
          while (!success) {
            try {
              sample =
                  enaSampleToBioSampleConversionService.enrichSample(
                      accession, false, oldStructuredData, newStructuredData);

              bioSamplesWebinClient.persistSampleResource(sample);

              success = true;
            } catch (final Exception e) {
              if (++numRetry == MAX_RETRIES) {
                throw new RuntimeException(
                    "Failed to handle the sample with accession " + accession);
              }
            }
          }
        }
      } catch (final Exception e) {
        log.info("Failed to handle ENA sample with accession " + accession, e);
      }

      return null;
    }
  }
}
