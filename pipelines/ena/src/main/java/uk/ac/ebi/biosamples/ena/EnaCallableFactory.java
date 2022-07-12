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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.ega.EgaSampleExporter;
import uk.ac.ebi.biosamples.model.structured.AbstractData;
import uk.ac.ebi.biosamples.model.structured.StructuredDataTable;

@Service
public class EnaCallableFactory {
  private final BioSamplesClient bioSamplesWebinClient;
  private final EnaSampleTransformationService enaSampleTransformationService;
  private final EgaSampleExporter egaSampleExporter;

  public EnaCallableFactory(
          @Qualifier("WEBINCLIENT") BioSamplesClient bioSamplesWebinClient,
          EnaSampleTransformationService enaSampleTransformationService,
          EgaSampleExporter egaSampleExporter) {
    this.bioSamplesWebinClient = bioSamplesWebinClient;
    this.enaSampleTransformationService = enaSampleTransformationService;
    this.egaSampleExporter = egaSampleExporter;
  }

  /**
   * Builds callable for dealing most ENA samples
   *
   * @param accession The accession passed
   * @param statusId sample status
   * @param suppressionHandler Is running to set samples to SUPPRESSED
   * @param amrData The AMR {@link AbstractData} of the sample
   * @return the callable, {@link EnaCallable}
   */
  public Callable<Void> build(
          String accession,
          String egaId,
          int statusId,
          boolean suppressionHandler,
          boolean killedHandler,
          Set<StructuredDataTable> amrData) {
    return new EnaCallable(
            accession,
            egaId,
            statusId,
            bioSamplesWebinClient,
            egaSampleExporter,
            enaSampleTransformationService,
            suppressionHandler,
            killedHandler,
            amrData);
  }
}
