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
import uk.ac.ebi.biosamples.model.SubmissionReceipt;

public class SampleSubmissionServiceV2 {
  private final Logger log = LoggerFactory.getLogger(getClass());
  private final RestOperations restOperations;
  private final URI uriV2;

  public SampleSubmissionServiceV2(final RestOperations restOperations, final URI uriV2) {
    this.restOperations = restOperations;
    this.uriV2 = uriV2;
  }

  public SubmissionReceipt submit(final List<Sample> samples) throws RestClientException {
    return new SampleSubmitterV2(samples).postSamples();
  }

  public SubmissionReceipt submit(final List<Sample> samples, final String jwt)
      throws RestClientException {
    return new SampleSubmitterV2(samples, jwt).postSamples();
  }

  public Map<String, String> accession(final List<Sample> samples) throws RestClientException {
    return new SampleAccessionerV2(samples).accessionSamples();
  }

  public Map<String, String> accession(final List<Sample> samples, final String jwt)
      throws RestClientException {
    return new SampleAccessionerV2(samples, jwt).accessionSamples();
  }

  private class SampleSubmitterV2 {
    private final List<Sample> samples;
    private final String jwt;

    SampleSubmitterV2(final List<Sample> samples) {
      this.samples = samples;
      jwt = null;
    }

    SampleSubmitterV2(final List<Sample> samples, final String jwt) {
      this.samples = samples;
      this.jwt = jwt;
    }

    public SubmissionReceipt postSamples() {
      final URI v2PostUri =
          UriComponentsBuilder.fromUri(URI.create(uriV2 + "/samples/bulk-submit"))
              .build(true)
              .toUri();
      log.info("POSTing {} samples {}", samples.size(), v2PostUri);

      final RequestEntity<List<Sample>> requestEntity =
          buildRequestEntityWithAuthHeader(v2PostUri, jwt, samples);
      final ResponseEntity<SubmissionReceipt> responseEntity;

      try {
        responseEntity = restOperations.exchange(requestEntity, SubmissionReceipt.class);
      } catch (final RestClientResponseException e) {
        log.error("Unable to POST to {} got response {}", v2PostUri, e.getResponseBodyAsString());

        throw e;
      }

      return responseEntity.getBody();
    }
  }

  private class SampleAccessionerV2 {
    private final List<Sample> samples;
    private final String jwt;

    SampleAccessionerV2(final List<Sample> samples) {
      this.samples = samples;
      jwt = null;
    }

    SampleAccessionerV2(final List<Sample> samples, final String jwt) {
      this.samples = samples;
      this.jwt = jwt;
    }

    public Map<String, String> accessionSamples() {
      final URI v2BulkAccessionUri =
          UriComponentsBuilder.fromUri(URI.create(uriV2 + "/samples" + "/bulk-accession"))
              .build(true)
              .toUri();
      log.info("Accessioning {} samples {}", samples.size(), v2BulkAccessionUri);

      final RequestEntity<List<Sample>> requestEntity =
          buildRequestEntityWithAuthHeader(v2BulkAccessionUri, jwt, samples);

      final ResponseEntity<Map<String, String>> responseEntity;

      try {
        responseEntity =
            restOperations.exchange(
                requestEntity, new ParameterizedTypeReference<Map<String, String>>() {});
      } catch (final RestClientResponseException e) {
        log.error(
            "Unable to accession samples from {} got response {}",
            v2BulkAccessionUri,
            e.getResponseBodyAsString());

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
