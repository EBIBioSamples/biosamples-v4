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
package uk.ac.ebi.biosamples.client.service;

import java.net.URI;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
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
  private final ExecutorService executor;
  private final RestOperations restOperations;
  private final boolean isWebinSubmission;

  public SampleCertificationService(
      RestOperations restOperations,
      Traverson traverson,
      ExecutorService executor,
      boolean isWebinSubmission) {
    this.restOperations = restOperations;
    this.traverson = traverson;
    this.executor = executor;
    this.isWebinSubmission = isWebinSubmission;
  }

  /** @param jwt json web token authorizing access to the domain the sample is assigned to */
  public Resource<Sample> submit(Sample sample, String jwt) throws RestClientException {
    try {
      return new CertifyCallable(sample, jwt).call();
    } catch (RestClientException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private class CertifyCallable implements Callable<Resource<Sample>> {
    private final Sample sample;
    private final String jwt;

    public CertifyCallable(Sample sample, String jwt) {
      this.sample = sample;
      this.jwt = jwt;
    }

    @Override
    public Resource<Sample> call() {
      PagedResources<Resource<Sample>> pagedSamples =
          traverson
              .follow("samples")
              .toObject(new ParameterizedTypeReference<PagedResources<Resource<Sample>>>() {});
      Link sampleLink = pagedSamples.getLink("sample");

      if (sampleLink == null) {
        log.warn("Problem handling page " + pagedSamples);
        throw new NullPointerException("Unable to find sample link");
      }

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

      ResponseEntity<Resource<Sample>> responseEntity;

      try {
        responseEntity =
            restOperations.exchange(
                requestEntity, new ParameterizedTypeReference<Resource<Sample>>() {});
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

    if (isWebinSubmission) {
      uriComponentsBuilder.queryParam("authProvider", "WEBIN");
    }

    return uriComponentsBuilder.build(true).toUri();
  }
}
