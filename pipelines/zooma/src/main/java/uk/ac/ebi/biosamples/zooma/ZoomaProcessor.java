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
package uk.ac.ebi.biosamples.zooma;

import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestOperations;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import uk.ac.ebi.biosamples.PipelinesProperties;
import uk.ac.ebi.biosamples.utils.ClientUtils;

@Service
public class ZoomaProcessor {

  private Logger log = LoggerFactory.getLogger(getClass());

  private final RestOperations restOperations;

  // TODO make this an application.properties value
  private final UriComponents uriBuilder;

  public ZoomaProcessor(
      RestTemplateBuilder restTemplateBuilder, PipelinesProperties pipelinesProperties) {
    this.restOperations = restTemplateBuilder.build();
    uriBuilder =
        UriComponentsBuilder.fromUriString(
                pipelinesProperties.getZooma()
                    + "/v2/api/services/annotate?propertyValue={value}&propertyType={type}&filter=ontologies:[none]")
            .build();
  }

  @Cacheable(value = "zooma", sync = true)
  public Optional<String> queryZooma(String type, String value) {
    log.trace("Zooma getting : " + type + " : " + value);
    URI uri = uriBuilder.expand(value, type).encode().toUri();
    // log.info("Zooma uri : "+url);

    RequestEntity<Void> requestEntity = RequestEntity.get(uri).accept(MediaTypes.HAL_JSON).build();
    long startTime = System.nanoTime();
    ResponseEntity<List<JsonNode>> responseEntity =
        ClientUtils.doRetryQuery(
            requestEntity, restOperations, 5, new ParameterizedTypeReference<List<JsonNode>>() {});
    long endTime = System.nanoTime();
    log.trace("Got zooma response in " + ((endTime - startTime) / 1000000) + "ms");

    // if zero or more than one result found, abort
    if (responseEntity.getBody().size() != 1) {
      log.info(
          "Zooma failed to map "
              + value
              + " ("
              + type
              + ") in "
              + ((endTime - startTime) / 1000000)
              + "ms");
      return Optional.empty();
    }
    JsonNode n = responseEntity.getBody().get(0);

    // if result is anything other than "high" confidence, abort
    if (!n.has("confidence") || !n.get("confidence").asText().equals("HIGH")) {
      log.info(
          "Zooma did not map "
              + value
              + " ("
              + type
              + ") in "
              + ((endTime - startTime) / 1000000)
              + "ms");
      return Optional.empty();
    }

    // if result has anything other than 1 semantic tag, abort
    if (!n.has("semanticTags") || n.get("semanticTags").size() != 1) {
      log.info(
          "Zooma multi-mapped "
              + value
              + " ("
              + type
              + ") in "
              + ((endTime - startTime) / 1000000)
              + "ms");
      return Optional.empty();
    }
    String iri = n.get("semanticTags").get(0).asText();
    log.info(
        "Zooma mapped "
            + value
            + " ("
            + type
            + ") to "
            + iri
            + " in "
            + ((endTime - startTime) / 1000000)
            + "ms");
    return Optional.of(iri);
  }
}
