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
import java.util.concurrent.ExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestOperations;
import org.springframework.web.util.UriComponentsBuilder;
import uk.ac.ebi.biosamples.model.Sample;

public class SampleRetrievalServiceV2 {
  private final Logger log = LoggerFactory.getLogger(getClass());

  private final ExecutorService executor;
  private final RestOperations restOperations;
  private final URI uriV2;

  public SampleRetrievalServiceV2(
      final RestOperations restOperations, final URI uriV2, final ExecutorService executor) {
    this.restOperations = restOperations;
    this.executor = executor;
    this.uriV2 = uriV2;
  }

  /** Accepts a accession and returns the sample */
  public Sample fetchSampleByAccession(final String accession) {
    return new FetchAccessionCallable(accession, uriV2).call();
  }

  /** Accepts a accession and returns the sample */
  public Sample fetchSampleByAccession(final String accession, final String jwt) {
    return new FetchAccessionCallable(accession, jwt, uriV2).call();
  }

  /**
   * Accepts a list of accessions and returns a Map having accession as key and sample as value
   *
   * @param accessions
   * @return
   */
  public Map<String, Sample> fetchSamplesByAccessions(final List<String> accessions) {
    return new FetchAccessionsCallable(accessions, uriV2).call();
  }

  /**
   * Accepts a list of accessions and returns a Map having accession as key and sample as value
   *
   * @param accessions
   * @return
   */
  public Map<String, Sample> fetchSamplesByAccessions(
      final List<String> accessions, final String jwt) {
    return new FetchAccessionsCallable(accessions, uriV2, jwt).call();
  }

  private class FetchAccessionsCallable {
    private final List<String> accessions;
    private final String jwt;
    private final URI uriV2;

    FetchAccessionsCallable(final List<String> accessions, final URI uriV2) {
      this.accessions = accessions;
      jwt = null;
      this.uriV2 = uriV2;
    }

    FetchAccessionsCallable(final List<String> accessions, final URI uriV2, final String jwt) {
      this.accessions = accessions;
      this.jwt = jwt;
      this.uriV2 = uriV2;
    }

    public Map<String, Sample> call() {
      final URI bulkFetchSamplesUri =
          UriComponentsBuilder.fromUri(URI.create(uriV2 + "/samples" + "/bulk-fetch"))
              .queryParam("accessions", String.join(",", accessions))
              .build(true)
              .toUri();

      log.info("GETing " + bulkFetchSamplesUri);

      final MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();

      headers.add(HttpHeaders.CONTENT_TYPE, MediaTypes.HAL_JSON.toString());

      if (jwt != null) {
        headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + jwt);
      }

      final RequestEntity<Void> requestEntity =
          new RequestEntity<>(headers, HttpMethod.GET, bulkFetchSamplesUri);
      final ResponseEntity<Map<String, Sample>> responseEntity;

      try {
        responseEntity =
            restOperations.exchange(
                requestEntity, new ParameterizedTypeReference<Map<String, Sample>>() {});
      } catch (final HttpStatusCodeException e) {
        if (e.getStatusCode().equals(HttpStatus.FORBIDDEN)
            || e.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
          return null;
        } else {
          throw e;
        }
      }

      log.trace("GETted " + bulkFetchSamplesUri);

      return responseEntity.getBody();
    }
  }

  private class FetchAccessionCallable {
    private final String accession;
    private final String jwt;
    private final URI uriV2;

    FetchAccessionCallable(final String accession, final String jwt, final URI uriV2) {
      this.accession = accession;
      this.jwt = jwt;
      this.uriV2 = uriV2;
    }

    FetchAccessionCallable(final String accession, final URI uriV2) {
      this.accession = accession;
      jwt = null;
      this.uriV2 = uriV2;
    }

    public Sample call() {
      final URI sampleGetUri =
          UriComponentsBuilder.fromUri(URI.create(uriV2 + "/samples" + "/" + accession))
              .build(true)
              .toUri();
      log.info("GETing " + sampleGetUri);

      final MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();

      headers.add(HttpHeaders.CONTENT_TYPE, MediaTypes.HAL_JSON.toString());

      if (jwt != null) {
        headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + jwt);
      }

      final RequestEntity<Void> requestEntity =
          new RequestEntity<>(headers, HttpMethod.GET, sampleGetUri);
      final ResponseEntity<Sample> responseEntity;

      try {
        responseEntity = restOperations.exchange(requestEntity, Sample.class);
      } catch (final HttpStatusCodeException e) {
        if (e.getStatusCode().equals(HttpStatus.FORBIDDEN)
            || e.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
          return null;
        } else {
          throw e;
        }
      }

      log.trace("GETted " + sampleGetUri);

      return responseEntity.getBody();
    }
  }
}
