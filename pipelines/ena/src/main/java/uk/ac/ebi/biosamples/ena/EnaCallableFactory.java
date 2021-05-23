/*
* Copyright 2019 EMBL - European Bioinformatics Institute
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
import uk.ac.ebi.biosamples.PipelinesProperties;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.structured.AbstractData;

@Service
public class EnaCallableFactory {
  private final BioSamplesClient bioSamplesClient;
  private final EnaXmlEnhancer enaXmlEnhancer;
  private final EnaElementConverter enaElementConverter;
  private final EraProDao eraProDao;
  private final String webinId;

  public EnaCallableFactory(
      @Qualifier("WEBINCLIENT") BioSamplesClient bioSamplesClient,
      EnaXmlEnhancer enaXmlEnhancer,
      EnaElementConverter enaElementConverter,
      EraProDao eraProDao,
      PipelinesProperties pipelinesProperties) {

    this.bioSamplesClient = bioSamplesClient;
    this.enaXmlEnhancer = enaXmlEnhancer;
    this.enaElementConverter = enaElementConverter;
    this.eraProDao = eraProDao;
    this.webinId = pipelinesProperties.getProxyWebinId();
  }

  /**
   * Builds callable for dealing most ENA samples
   *
   * @param accession The accession passed
   * @param statusId sample status
   * @param suppressionHandler Is running to set samples to SUPPRESSED
   * @param bsdAuthority Indicates its running for samples submitted through BioSamples
   * @param amrData The AMR {@link AbstractData} of the sample
   * @return the callable, {@link EnaCallable}
   */
  public Callable<Void> build(
      String accession,
      int statusId,
      boolean suppressionHandler,
      boolean killedHandler,
      boolean bsdAuthority,
      Set<AbstractData> amrData) {
    return new EnaCallable(
        accession,
        statusId,
        bioSamplesClient,
        enaXmlEnhancer,
        enaElementConverter,
        eraProDao,
        webinId,
        suppressionHandler,
        killedHandler,
        bsdAuthority,
        amrData);
  }
}
