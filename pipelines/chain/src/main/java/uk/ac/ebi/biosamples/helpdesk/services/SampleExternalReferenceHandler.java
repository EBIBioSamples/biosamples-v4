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
package uk.ac.ebi.biosamples.helpdesk.services;

import java.util.Collections;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.hateoas.EntityModel;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.PipelinesProperties;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.Curation;
import uk.ac.ebi.biosamples.model.ExternalReference;
import uk.ac.ebi.biosamples.model.Sample;

@Service
public class SampleExternalReferenceHandler {
  @Autowired
  @Qualifier("WEBINCLIENT")
  BioSamplesClient bioSamplesClient;

  @Autowired PipelinesProperties pipelinesProperties;

  public void processSample(final String accession) {
    final Optional<EntityModel<Sample>> optionalSampleEntityModel =
        bioSamplesClient.fetchSampleResource(accession);

    if (optionalSampleEntityModel.isPresent()) {
      final ExternalReference exRef =
          ExternalReference.build("https://www.ebi.ac.uk/ena/browser/view/" + accession);
      final Curation curation = Curation.build(null, null, null, Collections.singleton(exRef));

      bioSamplesClient.persistCuration(
          accession, curation, pipelinesProperties.getProxyWebinId(), true);
    }
  }
}
