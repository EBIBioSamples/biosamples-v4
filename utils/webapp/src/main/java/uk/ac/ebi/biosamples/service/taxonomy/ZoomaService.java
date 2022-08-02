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
package uk.ac.ebi.biosamples.service.taxonomy;

import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestOperations;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class ZoomaService {
  private static final String SEMANTIC_TAGS = "semanticTags";
  private static final String ZOOMA_BASE_URL =
      "https://www.ebi.ac.uk/spot/zooma/v2/api/services/annotate";
  private static final String NCBI_TAXON = "NCBITaxon";

  private final RestOperations restOperations;
  private final UriComponents uriBuilder;

  private static final Logger log = LoggerFactory.getLogger(ZoomaService.class);

  public ZoomaService() {
    this.restOperations =
        new RestTemplateBuilder(
                restTemplate -> {
                  // no cusomizations are required for us here - simple RestTemplate should work
                })
            .build();

    // Add filter in the URL to fetch only ncbitaxon
    uriBuilder =
        UriComponentsBuilder.fromUriString(
                ZOOMA_BASE_URL
                    + "?propertyValue={value}&propertyType={type}]&filter=ontologies:[ncbitaxon]")
            .build();
  }

  public Optional<String> queryZooma(String type, String value) {
    log.trace("Zooma getting : " + type + " : " + value);

    URI uri = uriBuilder.expand(value, type).encode().toUri();

    RequestEntity<Void> requestEntity = RequestEntity.get(uri).accept(MediaTypes.HAL_JSON).build();
    long startTime = System.nanoTime();

    ResponseEntity<List<JsonNode>> responseEntity =
        restOperations.exchange(requestEntity, new ParameterizedTypeReference<List<JsonNode>>() {});

    long endTime = System.nanoTime();

    log.trace("Got zooma response in " + ((endTime - startTime) / 1000000) + "ms");

    List<JsonNode> jsonNodes = responseEntity.getBody();

    return jsonNodes.stream()
        .map(
            jsonNode -> {
              AtomicReference<String> ncbiSemanticTag = new AtomicReference<>(null);

              jsonNode
                  .get(SEMANTIC_TAGS)
                  .forEach(
                      semanticTag -> {
                        final String semanticTagText = semanticTag.asText();

                        // fetching only ncbitaxon, but still cross check if there is anything else,
                        // accept only ncbitaxon
                        if (semanticTagText.contains(NCBI_TAXON)) {
                          ncbiSemanticTag.set(semanticTagText);
                        }
                      });

              return ncbiSemanticTag.get();
            })
        .filter(Objects::nonNull)
        .findFirst();
  }
}
