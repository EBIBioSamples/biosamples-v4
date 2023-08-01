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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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
  private final ExecutorService executor;
  private final RestOperations restOperations;

  public SampleSubmissionService(final RestOperations restOperations, final Traverson traverson) {
    this.restOperations = restOperations;
    this.traverson = traverson;
    this.executor = Executors.newSingleThreadExecutor();
  }

  /**
   * This will send the sample to biosamples, either by POST if it has no accession or by PUT if the
   * sample already has an accession associated
   *
   * <p>This method will wait for the request to complete before returning
   *
   * @param sample sample to be submitted
   * @return sample wrapped in resource
   */
  public EntityModel<Sample> submit(final Sample sample, final Boolean setFullDetails)
      throws RestClientException {
    try {
      return new SubmitCallable(sample, setFullDetails).call();
    } catch (final RestClientException e) {
      throw e;
    } catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }

  /** @param jwt json web token authorizing access to the domain the sample is assigned to */
  public EntityModel<Sample> submit(
      final Sample sample, final String jwt, final Boolean setFullDetails)
      throws RestClientException {
    try {
      return new SubmitCallable(sample, jwt, setFullDetails).call();
    } catch (final RestClientException e) {
      throw e;
    } catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * This will send the sample to biosamples, either by POST if it has no accession or by PUT if the
   * sample already has an accession associated
   *
   * <p>This will use a thread-pool within the client to do this asyncronously
   *
   * @param sample sample to be submitted
   * @return sample wrapped in resource
   */
  public Future<EntityModel<Sample>> submitAsync(final Sample sample, final Boolean setFullDetails)
      throws RestClientException {
    return executor.submit(new SubmitCallable(sample, setFullDetails));
  }

  /** @param jwt json web token authorizing access to the domain the sample is assigned to */
  public Future<EntityModel<Sample>> submitAsync(
      final Sample sample, final String jwt, final Boolean setFullDetails)
      throws RestClientException {
    return executor.submit(new SubmitCallable(sample, jwt, setFullDetails));
  }

  private class SubmitCallable implements Callable<EntityModel<Sample>> {
    private final Sample sample;
    private final Boolean setFullDetails;
    private final String jwt;

    SubmitCallable(final Sample sample, final Boolean setFullDetails) {
      this.sample = sample;
      this.setFullDetails = setFullDetails;
      jwt = null;
    }

    SubmitCallable(final Sample sample, final String jwt, final boolean setFullDetails) {
      this.sample = sample;
      this.setFullDetails = setFullDetails;
      this.jwt = jwt;
    }

    @Override
    public EntityModel<Sample> call() {
      // if the sample has an accession, put to that
      if (sample.getAccession() != null) {
        // samples with an existing accession should be PUT

        // don't do all this in traverson because it will get the end and then use the self
        // link
        // because we might PUT to something that doesn't exist (e.g. migration of data)
        // this will cause an error. So instead manually de-template the link without
        // getting it.
        final PagedModel<EntityModel<Sample>> pagedSamples =
            traverson
                .follow("samples")
                .toObject(new ParameterizedTypeReference<PagedModel<EntityModel<Sample>>>() {});
        Link sampleLink = pagedSamples.getLink("sample").get();

        sampleLink = sampleLink.expand(sample.getAccession());
        final URI uri = getSamplePersistURI(sampleLink);
        log.trace("PUTing to " + uri + " " + sample);

        final RequestEntity.BodyBuilder bodyBuilder =
            RequestEntity.put(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaTypes.HAL_JSON);
        if (jwt != null) {
          bodyBuilder.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt);
        }
        final RequestEntity<Sample> requestEntity = bodyBuilder.body(sample);

        final ResponseEntity<EntityModel<Sample>> responseEntity;
        try {
          responseEntity =
              restOperations.exchange(
                  requestEntity, new ParameterizedTypeReference<EntityModel<Sample>>() {});
        } catch (final RestClientResponseException e) {
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

      } else {
        // samples without an existing accession should be POST
        final Link sampleLink = traverson.follow("samples").asLink();
        final URI uri = getSamplePersistURI(sampleLink);
        log.trace("POSTing to " + uri + " " + sample);

        final RequestEntity.BodyBuilder bodyBuilder =
            RequestEntity.post(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaTypes.HAL_JSON);
        if (jwt != null) {
          bodyBuilder.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt);
        }
        final RequestEntity<Sample> requestEntity = bodyBuilder.body(sample);

        final ResponseEntity<EntityModel<Sample>> responseEntity;
        try {
          responseEntity =
              restOperations.exchange(
                  requestEntity, new ParameterizedTypeReference<EntityModel<Sample>>() {});
        } catch (final RestClientResponseException e) {
          log.error(
              "Unable to POST to "
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

    private URI getSamplePersistURI(final Link sampleLink) {
      final UriComponentsBuilder uriComponentsBuilder =
          UriComponentsBuilder.fromUriString(sampleLink.getHref());
      if (setFullDetails != null) {
        uriComponentsBuilder.queryParam("setfulldetails", setFullDetails);
      }

      return uriComponentsBuilder.build(true).toUri();
    }
  }
}
