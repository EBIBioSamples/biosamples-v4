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
package uk.ac.ebi.biosamples.ncbi;

import java.util.Map;
import java.util.Set;
import org.dom4j.Element;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.PipelinesProperties;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.structured.StructuredDataTable;
import uk.ac.ebi.biosamples.ncbi.service.NcbiSampleConversionService;

@Service
public class NcbiElementCallableFactory {
  private final BioSamplesClient bioSamplesClient;
  private final String webinId;
  private final NcbiSampleConversionService conversionService;

  public NcbiElementCallableFactory(
      final NcbiSampleConversionService conversionService,
      @Qualifier("WEBINCLIENT") final BioSamplesClient bioSamplesClient,
      final PipelinesProperties pipelinesProperties) {
    this.conversionService = conversionService;
    this.bioSamplesClient = bioSamplesClient;
    this.webinId = pipelinesProperties.getProxyWebinId();
  }

  public NcbiElementCallable build(
      final Element element, final Map<String, Set<StructuredDataTable>> sampleToAmrMap) {
    return new NcbiElementCallable(
        conversionService, bioSamplesClient, element, webinId, sampleToAmrMap);
  }
}
