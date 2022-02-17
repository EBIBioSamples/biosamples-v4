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

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.PipelinesProperties;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.ega.EgaSampleExporter;
import uk.ac.ebi.biosamples.model.structured.AbstractData;
import uk.ac.ebi.biosamples.model.structured.StructuredDataTable;

import java.util.Set;
import java.util.concurrent.Callable;

@Service
public class EnaCallableFactory {
  private final BioSamplesClient bioSamplesWebinClient;
  private final BioSamplesClient bioSamplesAapClient;
  private final EnaXmlEnhancer enaXmlEnhancer;
  private final EnaElementConverter enaElementConverter;
  private final EgaSampleExporter egaSampleExporter;
  private final EraProDao eraProDao;
  private final String webinId;

  public EnaCallableFactory(
      @Qualifier("WEBINCLIENT") BioSamplesClient bioSamplesWebinClient,
      BioSamplesClient bioSamplesAapClient,
      EnaXmlEnhancer enaXmlEnhancer,
      EnaElementConverter enaElementConverter,
      EgaSampleExporter egaSampleExporter,
      EraProDao eraProDao,
      PipelinesProperties pipelinesProperties) {
    this.bioSamplesWebinClient = bioSamplesWebinClient;
    this.bioSamplesAapClient = bioSamplesAapClient;
    this.enaXmlEnhancer = enaXmlEnhancer;
    this.enaElementConverter = enaElementConverter;
    this.egaSampleExporter = egaSampleExporter;
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
      String egaId,
      int statusId,
      boolean suppressionHandler,
      boolean killedHandler,
      boolean bsdAuthority,
      Set<StructuredDataTable> amrData) {
    return new EnaCallable(
        accession,
        egaId,
        statusId,
        bioSamplesWebinClient,
        bioSamplesAapClient,
        enaXmlEnhancer,
        enaElementConverter,
        egaSampleExporter,
        eraProDao,
        webinId,
        suppressionHandler,
        killedHandler,
        bsdAuthority,
        amrData);
  }
}
