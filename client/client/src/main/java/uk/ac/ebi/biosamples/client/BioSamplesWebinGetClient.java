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
package uk.ac.ebi.biosamples.client;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.annotation.PreDestroy;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.client.Traverson;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import uk.ac.ebi.biosamples.client.service.ClientService;
import uk.ac.ebi.biosamples.client.service.SampleRetrievalServiceV2;
import uk.ac.ebi.biosamples.client.service.WebinAuthClientService;
import uk.ac.ebi.biosamples.client.utils.ClientProperties;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.service.SampleValidator;

public class BioSamplesWebinGetClient implements AutoCloseable {
  private final Logger log = LoggerFactory.getLogger(getClass());
  private final SampleRetrievalServiceV2 sampleRetrievalServiceV2;
  /** -- GETTER -- Gets the public client. */
  @Getter private final Optional<BioSamplesClient> publicClient;

  /**
   * Constructs a BioSamplesClient with the given parameters.
   *
   * @param uri the URI for BioSamples
   * @param uriV2 the URI for BioSamples V2
   * @param restTemplateBuilder the RestTemplateBuilder
   * @param sampleValidator the SampleValidator
   * @param clientService the ClientService
   * @param clientProperties the BioSamplesProperties
   */
  public BioSamplesWebinGetClient(
      final URI uri,
      URI uriV2,
      final RestTemplateBuilder restTemplateBuilder,
      final SampleValidator sampleValidator,
      final ClientService clientService,
      final ClientProperties clientProperties) {
    if (uriV2 == null) {
      uriV2 = UriComponentsBuilder.fromUri(URI.create(uri + "/v2")).build().toUri();
    }

    final RestTemplate restOperations = restTemplateBuilder.build();

    if (clientService != null) {
      if (clientService instanceof WebinAuthClientService) {
        log.trace("Adding WebinClientHttpRequestInterceptor");
        restOperations
            .getInterceptors()
            .add(new BioSamplesWebinGetClient.BsdClientHttpRequestInterceptor(clientService));
      } else {
        log.trace("No ClientService available");
      }
    }

    final Traverson traverson = new Traverson(uri, MediaTypes.HAL_JSON);
    traverson.setRestOperations(restOperations);

    sampleRetrievalServiceV2 = new SampleRetrievalServiceV2(restOperations, uriV2);

    if (clientService == null) {
      publicClient = Optional.empty();
    } else {
      publicClient =
          Optional.of(
              new BioSamplesClient(
                  uri, uriV2, restTemplateBuilder, sampleValidator, null, clientProperties));
    }
  }

  private static class BsdClientHttpRequestInterceptor implements ClientHttpRequestInterceptor {
    private final ClientService clientService;

    BsdClientHttpRequestInterceptor(final ClientService clientService) {
      this.clientService = clientService;
    }

    @Override
    public ClientHttpResponse intercept(
        final HttpRequest request, final byte[] body, final ClientHttpRequestExecution execution)
        throws IOException {
      if (clientService != null && !request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
        final String jwt = clientService.getJwt();
        if (jwt != null) {
          request.getHeaders().set(HttpHeaders.AUTHORIZATION, "Bearer " + jwt);
        }
      }

      // pass along to the next interceptor
      return execution.execute(request, body);
    }
  }

  // TODO: we can think of using an interceptor to remove the cache from the biosamples client
  @Override
  @PreDestroy
  public void close() {
    // close down public client if present
    publicClient.ifPresent(BioSamplesClient::close);
    // close down our own thread pools
    // threadPoolExecutor.shutdownNow();
    try {
      // threadPoolExecutor.awaitTermination(1, TimeUnit.MINUTES);
    } catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Fetches sample resources by accessions using BioSamples V2.
   *
   * @param accessions the list of accessions
   * @return a map of sample accessions to samples
   * @throws RestClientException if there is an error while fetching samples
   */
  public Map<String, Sample> fetchSampleResourcesByAccessionsV2(final List<String> accessions)
      throws RestClientException {
    try {
      return sampleRetrievalServiceV2.fetchSamplesByAccessions(accessions);
    } catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Fetches sample resources by accessions using BioSamples V2 with JWT authentication.
   *
   * @param accessions the list of accessions
   * @param jwt the JWT token
   * @return a map of sample accessions to samples
   * @throws RestClientException if there is an error while fetching samples
   */
  public Map<String, Sample> fetchSampleResourcesByAccessionsV2(
      final List<String> accessions, final String jwt) throws RestClientException {
    try {
      return sampleRetrievalServiceV2.fetchSamplesByAccessions(accessions, jwt);
    } catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Fetches a sample resource by accession using BioSamples V2.
   *
   * @param accession the accession of the sample
   * @return the sample
   * @throws RestClientException if there is an error while fetching the sample
   */
  public Sample fetchSampleResourceV2(final String accession) throws RestClientException {
    try {
      return sampleRetrievalServiceV2.fetchSampleByAccession(accession);
    } catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Fetches a sample resource by accession using BioSamples V2 with JWT authentication.
   *
   * @param accession the accession of the sample
   * @param jwt the JWT token
   * @return the sample
   * @throws RestClientException if there is an error while fetching the sample
   */
  public Sample fetchSampleResourceV2(final String accession, final String jwt)
      throws RestClientException {
    try {
      return sampleRetrievalServiceV2.fetchSampleByAccession(accession, jwt);
    } catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }
}
