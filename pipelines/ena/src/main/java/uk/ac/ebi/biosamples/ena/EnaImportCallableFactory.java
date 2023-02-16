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

import java.util.concurrent.Callable;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.ega.EgaSampleExporter;

@Service
public class EnaImportCallableFactory {
  private final BioSamplesClient bioSamplesWebinClient;
  private final EnaSampleToBioSampleConversionService enaSampleToBioSampleConversionService;
  private final EgaSampleExporter egaSampleExporter;

  public EnaImportCallableFactory(
      @Qualifier("WEBINCLIENT") final BioSamplesClient bioSamplesWebinClient,
      final EnaSampleToBioSampleConversionService enaSampleToBioSampleConversionService,
      final EgaSampleExporter egaSampleExporter) {
    this.bioSamplesWebinClient = bioSamplesWebinClient;
    this.enaSampleToBioSampleConversionService = enaSampleToBioSampleConversionService;
    this.egaSampleExporter = egaSampleExporter;
  }

  /**
   * Builds callable for dealing most ENA samples
   *
   * @param accession The accession passed
   * @return the callable, {@link EnaImportCallable}
   */
  public Callable<Void> build(final String accession, final String egaId) {
    return new EnaImportCallable(
        accession,
        egaId,
        bioSamplesWebinClient,
        egaSampleExporter,
        enaSampleToBioSampleConversionService);
  }
}
