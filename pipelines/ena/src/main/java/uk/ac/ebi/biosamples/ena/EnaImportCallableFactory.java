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
import uk.ac.ebi.biosamples.service.EnaSampleToBioSampleConversionService;
import uk.ac.ebi.biosamples.service.EraProDao;

@Service
public class EnaImportCallableFactory {
  private final BioSamplesClient bioSamplesWebinClient;
  private final BioSamplesClient bioSamplesAapClient;
  private final EnaSampleToBioSampleConversionService enaSampleToBioSampleConversionService;
  private final EraProDao eraProDao;

  public EnaImportCallableFactory(
      @Qualifier("WEBINCLIENT") final BioSamplesClient bioSamplesWebinClient,
      final BioSamplesClient bioSamplesAapClient,
      final EnaSampleToBioSampleConversionService enaSampleToBioSampleConversionService,
      final EraProDao eraProDao) {
    this.bioSamplesWebinClient = bioSamplesWebinClient;
    this.bioSamplesAapClient = bioSamplesAapClient;
    this.enaSampleToBioSampleConversionService = enaSampleToBioSampleConversionService;
    this.eraProDao = eraProDao;
  }

  /**
   * Builds callable for dealing most ENA samples
   *
   * @param accession The accession passed
   * @return the callable, {@link EnaImportCallable}
   */
  public Callable<Void> build(final String accession) {
    return new EnaImportCallable(
        accession,
        bioSamplesWebinClient,
        bioSamplesAapClient,
        enaSampleToBioSampleConversionService,
        eraProDao,
        null);
  }

  /**
   * Builds callable for dealing most ENA samples
   *
   * @param accession The accession passed
   * @return the callable, {@link EnaImportCallable}
   */
  public Callable<Void> build(final String accession, final SpecialTypes specialTypes) {
    return new EnaImportCallable(
        accession,
        bioSamplesWebinClient,
        bioSamplesAapClient,
        enaSampleToBioSampleConversionService,
        eraProDao,
        specialTypes);
  }
}
