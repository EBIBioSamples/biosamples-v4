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

import java.util.Set;
import java.util.concurrent.Callable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.ega.EgaSampleExporter;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.structured.StructuredDataTable;

public class EnaCallable implements Callable<Void> {
  private final Logger log = LoggerFactory.getLogger(getClass());
  private static final int MAX_RETRIES = 5;

  private final String accession;
  private final String egaId;
  private final BioSamplesClient bioSamplesWebinClient;
  private final EgaSampleExporter egaSampleExporter;
  private final EnaSampleTransformationService enaSampleTransformationService;
  private final Set<StructuredDataTable> amrData;

  public EnaCallable(
      String accession,
      String egaId,
      BioSamplesClient bioSamplesWebinClient,
      EgaSampleExporter egaSampleExporter,
      EnaSampleTransformationService enaSampleTransformationService,
      Set<StructuredDataTable> amrData) {
    this.accession = accession;
    this.egaId = egaId;
    this.bioSamplesWebinClient = bioSamplesWebinClient;
    this.egaSampleExporter = egaSampleExporter;
    this.enaSampleTransformationService = enaSampleTransformationService;
    this.amrData = amrData;
  }

  @Override
  public Void call() {
    if (egaId != null && !egaId.isEmpty()) {
      return egaSampleExporter.populateAndSubmitEgaData(egaId);
    } else {
      try {
        boolean success = false;
        int numRetry = 0;

        final Sample sample = enaSampleTransformationService.enrichSample(this.accession, false);

        while (!success) {
          try {
            bioSamplesWebinClient.persistSampleResource(sample);

            success = true;
          } catch (final Exception e) {
            if (++numRetry == MAX_RETRIES) {
              throw new RuntimeException(
                  "Failed to handle the sample with accession " + this.accession);
            }

            success = false;
          }
        }
      } catch (final Exception e) {
        log.info("Failed to handle ENA sample with accession " + this.accession, e);
      }

      return null;
    }
  }
}
