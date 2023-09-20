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
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestOperations;
import org.springframework.web.util.UriComponentsBuilder;
import uk.ac.ebi.biosamples.model.Sample;

public class SampleSubmissionServiceV2 {
  private final Logger log = LoggerFactory.getLogger(getClass());

  private final RestOperations restOperations;
  private final URI uriV2;

  public SampleSubmissionServiceV2(final RestOperations restOperations, final URI uriV2) {
    this.restOperations = restOperations;
    this.uriV2 = uriV2;
  }

  /**
   * This will POST a list of samples to BioSamples
   *
   * <p>This will use a thread-pool within the client to do this asynchronously
   *
   * @param samples samples to be submitted
   * @return sample wrapped in resource
   */
  public List<Sample> postAsync(final List<Sample> samples) throws RestClientException {
    return new PostSamplesV2(samples).postSamples();
  }

  /**
   * This will POST a list of samples to BioSamples
   *
   * <p>This will use a thread-pool within the client to do this asynchronously
   *
   * @param samples samples to be submitted
   * @return sample wrapped in resource
   */
  public List<Sample> postAsync(final List<Sample> samples, final String jwt)
      throws RestClientException {
    return new PostSamplesV2(samples, jwt).postSamples();
  }

  public Map<String, String> bulkAccessionAsync(final List<Sample> samples)
      throws RestClientException {
    return new AccessionSamplesV2(samples).accessionSamples();
  }

  public Map<String, String> bulkAccessionAsync(final List<Sample> samples, final String jwt)
      throws RestClientException {
    return new AccessionSamplesV2(samples, jwt).accessionSamples();
  }

  private class PostSamplesV2 {
    private final List<Sample> samples;
    private final String jwt;

    PostSamplesV2(final List<Sample> samples) {
      this.samples = samples;
      jwt = null;
    }

    PostSamplesV2(final List<Sample> samples, final String jwt) {
      this.samples = samples;
      this.jwt = jwt;
    }

    public List<Sample> postSamples() {
      final URI v2PostUri =
          UriComponentsBuilder.fromUri(URI.create(uriV2 + "/samples/bulk-submit"))
              .build(true)
              .toUri();
      log.info("POSTing " + samples.size() + " samples " + v2PostUri);

      final RequestEntity<List<Sample>> requestEntity =
          buildRequestEntityWithAuthHeader(v2PostUri, jwt, samples);
      final ResponseEntity<List<Sample>> responseEntity;

      try {
        responseEntity =
            restOperations.exchange(requestEntity, new ParameterizedTypeReference<>() {});
      } catch (final RestClientResponseException e) {
        log.error(
            "Unable to POST to " + v2PostUri + " got response " + e.getResponseBodyAsString());
        throw e;
      }

      return responseEntity.getBody();
    }
  }

  private class AccessionSamplesV2 {
    private final List<Sample> samples;
    private final String jwt;

    AccessionSamplesV2(final List<Sample> samples) {
      this.samples = samples;
      jwt = null;
    }

    AccessionSamplesV2(final List<Sample> samples, final String jwt) {
      this.samples = samples;
      this.jwt = jwt;
    }

    public Map<String, String> accessionSamples() {
      final URI v2BulkAccessionUri =
          UriComponentsBuilder.fromUri(URI.create(uriV2 + "/samples" + "/bulk-accession"))
              .build(true)
              .toUri();
      log.info("Accessioning " + samples.size() + " samples " + v2BulkAccessionUri);

      final RequestEntity<List<Sample>> requestEntity =
          buildRequestEntityWithAuthHeader(v2BulkAccessionUri, jwt, samples);

      final ResponseEntity<Map<String, String>> responseEntity;

      try {
        responseEntity =
            restOperations.exchange(
                requestEntity, new ParameterizedTypeReference<Map<String, String>>() {});
      } catch (final RestClientResponseException e) {
        log.error(
            "Unable to accession samples from "
                + v2BulkAccessionUri
                + " got response "
                + e.getResponseBodyAsString());
        throw e;
      }

      return responseEntity.getBody();
    }
  }

  private RequestEntity<List<Sample>> buildRequestEntityWithAuthHeader(
      final URI uri, final String jwt, final List<Sample> samples) {
    final RequestEntity.BodyBuilder bodyBuilder =
        RequestEntity.post(uri).contentType(MediaType.APPLICATION_JSON).accept(MediaTypes.HAL_JSON);

    if (jwt != null) {
      bodyBuilder.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt);
    }

    return bodyBuilder.body(samples);
  }
}
