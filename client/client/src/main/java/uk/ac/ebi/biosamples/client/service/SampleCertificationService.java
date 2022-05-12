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
import java.util.concurrent.Callable;
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

public class SampleCertificationService {
  private final Logger log = LoggerFactory.getLogger(getClass());

  private final Traverson traverson;
  private final RestOperations restOperations;

  public SampleCertificationService(RestOperations restOperations, Traverson traverson) {
    this.restOperations = restOperations;
    this.traverson = traverson;
  }

  /** @param jwt json web token authorizing access to the domain the sample is assigned to */
  public EntityModel<Sample> submit(Sample sample, String jwt) throws RestClientException {
    try {
      return new CertifyCallable(sample, jwt).call();
    } catch (RestClientException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private class CertifyCallable implements Callable<EntityModel<Sample>> {
    private final Sample sample;
    private final String jwt;

    public CertifyCallable(Sample sample, String jwt) {
      this.sample = sample;
      this.jwt = jwt;
    }

    @Override
    public EntityModel<Sample> call() {
      PagedModel<EntityModel<Sample>> pagedSamples =
          traverson
              .follow("samples")
              .toObject(new ParameterizedTypeReference<PagedModel<EntityModel<Sample>>>() {});
      Link sampleLink = pagedSamples.getLink("sample").get();

      sampleLink = sampleLink.expand(sample.getAccession());

      URI uri = getSampleCertificationURI(sampleLink);

      log.info("PUTing to " + uri + " " + sample);

      RequestEntity.BodyBuilder bodyBuilder =
          RequestEntity.put(uri)
              .contentType(MediaType.APPLICATION_JSON)
              .accept(MediaTypes.HAL_JSON);
      if (jwt != null) {
        bodyBuilder.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt);
      }

      RequestEntity<Sample> requestEntity = bodyBuilder.body(sample);

      ResponseEntity<EntityModel<Sample>> responseEntity;

      try {
        responseEntity =
            restOperations.exchange(
                requestEntity, new ParameterizedTypeReference<EntityModel<Sample>>() {});
      } catch (RestClientResponseException e) {
        log.error(
            "Unable to PUT to "
                + uri
                + " body "
                + sample
                + " got response "
                + e.getResponseBodyAsString());
        throw e;
      }

      return responseEntity.getBody();
    }
  }

  private URI getSampleCertificationURI(Link sampleLink) {
    UriComponentsBuilder uriComponentsBuilder =
        UriComponentsBuilder.fromUriString(sampleLink.getHref() + "/certify");

    return uriComponentsBuilder.build(true).toUri();
  }
}
