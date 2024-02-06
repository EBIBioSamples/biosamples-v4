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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.client.Traverson;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestOperations;
import org.springframework.web.util.UriComponentsBuilder;
import uk.ac.ebi.biosamples.model.Sample;

public class SampleSubmissionService {
  private final Logger log = LoggerFactory.getLogger(getClass());
  private final Traverson traverson;
  private final RestOperations restOperations;

  public SampleSubmissionService(final RestOperations restOperations, final Traverson traverson) {
    this.restOperations = restOperations;
    this.traverson = traverson;
  }

  public EntityModel<Sample> submit(final Sample sample) throws RestClientException {
    try {
      return new SubmitOperation(sample, null).submit();
    } catch (final RestClientException e) {
      throw e;
    } catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }

  public EntityModel<Sample> submit(final Sample sample, final String jwt)
      throws RestClientException {
    try {
      return new SubmitOperation(sample, jwt).submit();
    } catch (final RestClientException e) {
      throw e;
    } catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }

  private class SubmitOperation {
    private final Sample sample;
    private final String jwt;

    SubmitOperation(final Sample sample, final String jwt) {
      this.sample = sample;
      this.jwt = jwt;
    }

    public EntityModel<Sample> submit() {
      if (sample.getAccession() != null) {
        return submitExistingAccession();
      } else {
        return submitNewAccession();
      }
    }

    private EntityModel<Sample> submitExistingAccession() {
      final PagedModel<EntityModel<Sample>> pagedSamples =
          traverson.follow("samples").toObject(new ParameterizedTypeReference<>() {});

      Link sampleLink = pagedSamples.getLink("sample").orElseThrow();
      sampleLink = sampleLink.expand(sample.getAccession());

      final URI uri = getSamplePersistURI(sampleLink);

      log.trace("PUTting to {} {}", uri, sample);

      final RequestEntity.BodyBuilder bodyBuilder =
          RequestEntity.put(uri)
              .contentType(MediaType.APPLICATION_JSON)
              .accept(MediaTypes.HAL_JSON);

      addAuthorizationHeader(bodyBuilder);

      final RequestEntity<Sample> requestEntity = bodyBuilder.body(sample);
      final ResponseEntity<EntityModel<Sample>> responseEntity =
          exchangeRequest(requestEntity, uri);

      return responseEntity.getBody();
    }

    private EntityModel<Sample> submitNewAccession() {
      final Link sampleLink = traverson.follow("samples").asLink();
      final URI uri = getSamplePersistURI(sampleLink);

      log.trace("POSTing to {} {}", uri, sample);

      final RequestEntity.BodyBuilder bodyBuilder =
          RequestEntity.post(uri)
              .contentType(MediaType.APPLICATION_JSON)
              .accept(MediaTypes.HAL_JSON);

      addAuthorizationHeader(bodyBuilder);

      final RequestEntity<Sample> requestEntity = bodyBuilder.body(sample);
      final ResponseEntity<EntityModel<Sample>> responseEntity =
          exchangeRequest(requestEntity, uri);

      return responseEntity.getBody();
    }

    private ResponseEntity<EntityModel<Sample>> exchangeRequest(
        final RequestEntity<Sample> requestEntity, final URI uri) {
      try {
        return restOperations.exchange(requestEntity, new ParameterizedTypeReference<>() {});
      } catch (final RestClientResponseException e) {
        log.error(
            "Unable to {} to {} body {} got response {}",
            requestEntity.getMethod(),
            uri,
            sample,
            e.getResponseBodyAsString());

        throw e;
      }
    }

    private void addAuthorizationHeader(final RequestEntity.HeadersBuilder<?> headersBuilder) {
      if (jwt != null) {
        headersBuilder.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt);
      }
    }

    private URI getSamplePersistURI(final Link sampleLink) {
      final UriComponentsBuilder uriComponentsBuilder =
          UriComponentsBuilder.fromUriString(sampleLink.getHref());

      return uriComponentsBuilder.build(true).toUri();
    }
  }
}
