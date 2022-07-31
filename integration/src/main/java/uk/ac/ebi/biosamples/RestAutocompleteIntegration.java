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
package uk.ac.ebi.biosamples;

import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestOperations;
import org.springframework.web.util.UriComponentsBuilder;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.Autocomplete;

@Component
@Order(4)
// @Profile({"default"})
public class RestAutocompleteIntegration extends AbstractIntegration {

  private Logger log = LoggerFactory.getLogger(this.getClass());

  private final IntegrationProperties integrationProperties;
  private final BioSamplesProperties bioSamplesProperties;
  private final RestOperations restTemplate;

  public RestAutocompleteIntegration(
      RestTemplateBuilder restTemplateBuilder,
      IntegrationProperties integrationProperties,
      BioSamplesProperties bioSamplesProperties,
      BioSamplesClient client) {
    super(client);
    this.restTemplate = restTemplateBuilder.build();
    this.integrationProperties = integrationProperties;
    this.bioSamplesProperties = bioSamplesProperties;
  }

  @Override
  protected void phaseOne() {}

  @Override
  protected void phaseTwo() {

    URI uri =
        UriComponentsBuilder.fromUri(bioSamplesProperties.getBiosamplesClientUri())
            .pathSegment("samples")
            .pathSegment("autocomplete")
            .build()
            .toUri();

    log.info("GETting from " + uri);
    RequestEntity<Void> request = RequestEntity.get(uri).accept(MediaType.APPLICATION_JSON).build();
    ResponseEntity<Autocomplete> response =
        restTemplate.exchange(request, new ParameterizedTypeReference<Autocomplete>() {});
    // check that there is at least one sample returned
    // if there are zero, then probably nothing was indexed
    if (response.getBody().getSuggestions().size() <= 0) {
      throw new RuntimeException("No autocomplete suggestions found!");
    }
  }

  @Override
  protected void phaseThree() {}

  @Override
  protected void phaseFour() {}

  @Override
  protected void phaseFive() {}

  @Override
  protected void phaseSix() {}
}
