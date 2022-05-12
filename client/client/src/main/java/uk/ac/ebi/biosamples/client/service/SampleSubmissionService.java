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
  private final boolean isWebinSubmission;

  public SampleSubmissionService(
      RestOperations restOperations,
      Traverson traverson,
      ExecutorService executor,
      boolean isWebinSubmission) {
    this.restOperations = restOperations;
    this.traverson = traverson;
    this.executor = executor;
    this.isWebinSubmission = isWebinSubmission;
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
  public EntityModel<Sample> submit(Sample sample, Boolean setFullDetails)
      throws RestClientException {
    try {
      return new SubmitCallable(sample, setFullDetails, isWebinSubmission).call();
    } catch (RestClientException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /** @param jwt json web token authorizing access to the domain the sample is assigned to */
  public EntityModel<Sample> submit(Sample sample, String jwt, Boolean setFullDetails)
      throws RestClientException {
    try {
      return new SubmitCallable(sample, jwt, setFullDetails, isWebinSubmission).call();
    } catch (RestClientException e) {
      throw e;
    } catch (Exception e) {
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
  public Future<EntityModel<Sample>> submitAsync(Sample sample, Boolean setFullDetails)
      throws RestClientException {
    return executor.submit(new SubmitCallable(sample, setFullDetails, isWebinSubmission));
  }

  /** @param jwt json web token authorizing access to the domain the sample is assigned to */
  public Future<EntityModel<Sample>> submitAsync(Sample sample, String jwt, Boolean setFullDetails)
      throws RestClientException {
    return executor.submit(new SubmitCallable(sample, jwt, setFullDetails, isWebinSubmission));
  }

  private class SubmitCallable implements Callable<EntityModel<Sample>> {
    private final Sample sample;
    private final Boolean setFullDetails;
    private final String jwt;
    private final Boolean isWebinSubmission;

    public SubmitCallable(Sample sample, Boolean setFullDetails, Boolean isWebinSubmission) {
      this.sample = sample;
      this.setFullDetails = setFullDetails;
      this.jwt = null;
      this.isWebinSubmission = isWebinSubmission;
    }

    public SubmitCallable(
        Sample sample, String jwt, boolean setFullDetails, Boolean isWebinSubmission) {
      this.sample = sample;
      this.setFullDetails = setFullDetails;
      this.jwt = jwt;
      this.isWebinSubmission = isWebinSubmission;
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
        PagedModel<EntityModel<Sample>> pagedSamples =
            traverson
                .follow("samples")
                .toObject(new ParameterizedTypeReference<PagedModel<EntityModel<Sample>>>() {});
        Link sampleLink = pagedSamples.getLink("sample").get();

        sampleLink = sampleLink.expand(sample.getAccession());
        URI uri = getSamplePersistURI(sampleLink);
        log.trace("PUTing to " + uri + " " + sample);

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

      } else {
        // samples without an existing accession should be POST
        Link sampleLink = traverson.follow("samples").asLink();
        URI uri = getSamplePersistURI(sampleLink);
        log.trace("POSTing to " + uri + " " + sample);

        RequestEntity.BodyBuilder bodyBuilder =
            RequestEntity.post(uri)
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

    private URI getSamplePersistURI(Link sampleLink) {
      UriComponentsBuilder uriComponentsBuilder =
          UriComponentsBuilder.fromUriString(sampleLink.getHref());
      if (setFullDetails != null) {
        uriComponentsBuilder.queryParam("setfulldetails", setFullDetails);
      }

      return uriComponentsBuilder.build(true).toUri();
    }
  }
}
