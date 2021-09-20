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

import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.PipelinesProperties;
import uk.ac.ebi.biosamples.client.BioSamplesClient;

@Service
public class NcbiCurationCallableFactory {
  private final BioSamplesClient bioSamplesClient;
  private final String domain;

  public NcbiCurationCallableFactory(
      BioSamplesClient bioSamplesClient, PipelinesProperties pipelinesProperties) {
    this.bioSamplesClient = bioSamplesClient;
    this.domain = pipelinesProperties.getEnaDomain();
  }

  /**
   * Builds a callable for dealing samples that are SUPPRESSED
   *
   * @param accession The accession passed
   * @param statusId The sample status
   * @param suppressionHandler true for this case
   * @return the callable, {@link NcbiCurationCallable}
   */
  public NcbiCurationCallable build(String accession, int statusId, boolean suppressionHandler) {
    return new NcbiCurationCallable(
        accession, statusId, bioSamplesClient, domain, suppressionHandler);
  }
}
