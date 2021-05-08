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
import java.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.MediaTypes;
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

public class SampleGroupSubmissionService {
  private static final Logger LOGGER = LoggerFactory.getLogger(SampleGroupSubmissionService.class);

  private final Traverson traverson;
  private final ExecutorService executor;
  private final RestOperations restOperations;

  public SampleGroupSubmissionService(
      RestOperations restOperations, Traverson traverson, ExecutorService executor) {
    this.restOperations = restOperations;
    this.traverson = traverson;
    this.executor = executor;
  }

  /**
   * @param sample sample to be submitted
   * @param jwt json web token authorizing access to the domain the sample is assigned to
   * @return sample wrapped in resource
   */
  public Resource<Sample> submit(Sample sample, String jwt) throws RestClientException {
    try {
      return new SubmitCallable(sample, jwt).call();
    } catch (RestClientException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * @param sample sample to be submitted
   * @param jwt json web token authorizing access to the domain the sample is assigned to
   * @return sample wrapped in resource
   */
  public Future<Resource<Sample>> submitAsync(Sample sample, String jwt)
      throws RestClientException {
    return executor.submit(new SubmitCallable(sample, jwt));
  }

  private class SubmitCallable implements Callable<Resource<Sample>> {
    private final Sample sample;
    private final String jwt;

    public SubmitCallable(Sample sample, String jwt) {
      this.sample = sample;
      this.jwt = jwt;
    }

    @Override
    public Resource<Sample> call() throws Exception {
      boolean update = sample.getAccession() != null;

      Link sampleLink = traverson.follow("groups").asLink();
      URI uri = getSamplePersistURI(sampleLink, sample.getAccession());

      RequestEntity.BodyBuilder bodyBuilder =
          update ? RequestEntity.put(uri) : RequestEntity.post(uri);
      bodyBuilder = bodyBuilder.contentType(MediaType.APPLICATION_JSON).accept(MediaTypes.HAL_JSON);
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
        LOGGER.error(
            "Failed to persist sample group, uri: {}, body: {}, response: {}",
            uri,
            sample,
            e.getResponseBodyAsString());
        throw e;
      }
      return responseEntity.getBody();
    }

    private URI getSamplePersistURI(Link sampleLink, String accession) {
      UriComponentsBuilder uriComponentsBuilder;
      if (accession == null) {
        uriComponentsBuilder = UriComponentsBuilder.fromUriString(sampleLink.getHref());
      } else {
        uriComponentsBuilder =
            UriComponentsBuilder.fromUriString(sampleLink.getHref() + "/" + accession);
      }
      return uriComponentsBuilder.build(true).toUri();
    }
  }
}
