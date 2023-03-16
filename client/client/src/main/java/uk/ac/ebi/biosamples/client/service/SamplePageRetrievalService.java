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
package uk.ac.ebi.biosamples.client.service;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.client.Traverson;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestOperations;
import org.springframework.web.util.UriComponentsBuilder;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.filter.Filter;

public class SamplePageRetrievalService {
  private final Logger log = LoggerFactory.getLogger(getClass());

  private final Traverson traverson;
  private final RestOperations restOperations;

  public SamplePageRetrievalService(
      final RestOperations restOperations, final Traverson traverson) {
    this.restOperations = restOperations;
    this.traverson = traverson;
  }

  public PagedModel<EntityModel<Sample>> search(
      final String text, final Collection<Filter> filters, final int page, final int size) {
    return search(text, filters, page, size, null);
  }

  public PagedModel<EntityModel<Sample>> search(
      final String text,
      final Collection<Filter> filters,
      final int page,
      final int size,
      final String jwt) {
    MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
    // TODO use shared constants here
    params.add("page", Integer.toString(page));
    params.add("size", Integer.toString(size));
    params.add("text", !text.isEmpty() ? text : "*:*");

    for (final Filter filter : filters) {
      params.add("filter", filter.getSerialization());
    }

    params = encodePlusInQueryParameters(params);

    final URI uri =
        UriComponentsBuilder.fromUriString(traverson.follow("samples").asLink().getHref())
            .queryParams(params)
            .build()
            .toUri();

    log.trace("GETing " + uri);

    final MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();

    headers.add(HttpHeaders.CONTENT_TYPE, MediaTypes.HAL_JSON.toString());

    if (jwt != null) {
      headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + jwt);
    }
    final RequestEntity<Void> requestEntity = new RequestEntity<>(headers, HttpMethod.GET, uri);

    final ResponseEntity<PagedModel<EntityModel<Sample>>> responseEntity =
        restOperations.exchange(
            requestEntity, new ParameterizedTypeReference<PagedModel<EntityModel<Sample>>>() {});

    if (!responseEntity.getStatusCode().is2xxSuccessful()) {
      throw new RuntimeException("Problem GETing samples");
    }

    log.trace("GETted " + uri);

    return responseEntity.getBody();
  }

  // TODO to keep the + in a (not encoded) query parameter is to force encoding
  private MultiValueMap<String, String> encodePlusInQueryParameters(
      final MultiValueMap<String, String> queryParameters) {
    final MultiValueMap<String, String> encodedQueryParameters = new LinkedMultiValueMap<>();
    for (final Map.Entry<String, List<String>> param : queryParameters.entrySet()) {
      final String key = param.getKey();
      param
          .getValue()
          .forEach(
              v -> {
                if (v != null) {
                  encodedQueryParameters.add(key, v.replaceAll("\\+", "%2B"));
                } else {
                  encodedQueryParameters.add(key, "");
                }
              });
    }
    return encodedQueryParameters;
  }
}
