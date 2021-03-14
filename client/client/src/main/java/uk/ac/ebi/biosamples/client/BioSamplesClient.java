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
package uk.ac.ebi.biosamples.client;

import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.client.Traverson;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import uk.ac.ebi.biosamples.BioSamplesProperties;
import uk.ac.ebi.biosamples.client.service.*;
import uk.ac.ebi.biosamples.model.Curation;
import uk.ac.ebi.biosamples.model.CurationLink;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.StaticViewWrapper;
import uk.ac.ebi.biosamples.model.filter.Filter;
import uk.ac.ebi.biosamples.service.SampleValidator;
import uk.ac.ebi.biosamples.utils.AdaptiveThreadPoolExecutor;

/**
 * This is the primary class for interacting with BioSamples.
 *
 * @author faulcon
 */
public class BioSamplesClient implements AutoCloseable {

  private final Logger log = LoggerFactory.getLogger(getClass());

  private final SampleRetrievalService sampleRetrievalService;
  private final SamplePageRetrievalService samplePageRetrievalService;
  private final SampleCursorRetrievalService sampleCursorRetrievalService;
  private final SampleSubmissionService sampleSubmissionService;
  private final SampleGroupSubmissionService sampleGroupSubmissionService;
  private final CurationRetrievalService curationRetrievalService;
  private final CurationSubmissionService curationSubmissionService;
  private final SampleCertificationService sampleCertificationService;

  private final SampleValidator sampleValidator;

  private final ExecutorService threadPoolExecutor;

  private final Optional<BioSamplesClient> publicClient;

  public BioSamplesClient(
      URI uri,
      RestTemplateBuilder restTemplateBuilder,
      SampleValidator sampleValidator,
      AapClientService aapClientService,
      BioSamplesProperties bioSamplesProperties) {

    RestTemplate restOperations = restTemplateBuilder.build();

    threadPoolExecutor =
        AdaptiveThreadPoolExecutor.create(
            100,
            10000,
            true,
            bioSamplesProperties.getBiosamplesClientThreadCount(),
            bioSamplesProperties.getBiosamplesClientThreadCountMax());

    if (aapClientService != null) {
      log.trace("Adding AapClientHttpRequestInterceptor");
      restOperations.getInterceptors().add(new AapClientHttpRequestInterceptor(aapClientService));
    } else {
      log.trace("No AapClientService available");
    }

    Traverson traverson = new Traverson(uri, MediaTypes.HAL_JSON);
    traverson.setRestOperations(restOperations);

    sampleRetrievalService =
        new SampleRetrievalService(restOperations, traverson, threadPoolExecutor);
    samplePageRetrievalService =
        new SamplePageRetrievalService(
            restOperations,
            traverson,
            threadPoolExecutor,
            bioSamplesProperties.getBiosamplesClientPagesize());
    sampleCursorRetrievalService =
        new SampleCursorRetrievalService(
            restOperations,
            traverson,
            threadPoolExecutor,
            bioSamplesProperties.getBiosamplesClientPagesize());

    sampleSubmissionService =
        new SampleSubmissionService(restOperations, traverson, threadPoolExecutor);

    sampleCertificationService =
        new SampleCertificationService(restOperations, traverson, threadPoolExecutor);

    sampleGroupSubmissionService =
        new SampleGroupSubmissionService(restOperations, traverson, threadPoolExecutor);
    curationRetrievalService =
        new CurationRetrievalService(
            restOperations,
            traverson,
            threadPoolExecutor,
            bioSamplesProperties.getBiosamplesClientPagesize());
    curationSubmissionService =
        new CurationSubmissionService(restOperations, traverson, threadPoolExecutor);

    this.sampleValidator = sampleValidator;

    if (aapClientService == null) {
      this.publicClient = Optional.empty();
    } else {
      this.publicClient =
          Optional.of(
              new BioSamplesClient(
                  uri, restTemplateBuilder, sampleValidator, null, bioSamplesProperties));
    }
  }

  public Optional<BioSamplesClient> getPublicClient() {
    return this.publicClient;
  }

  private static class AapClientHttpRequestInterceptor implements ClientHttpRequestInterceptor {

    private final AapClientService aapClientService;

    public AapClientHttpRequestInterceptor(AapClientService aapClientService) {
      this.aapClientService = aapClientService;
    }

    @Override
    public ClientHttpResponse intercept(
        HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
      if (aapClientService != null
          && !request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
        String jwt = aapClientService.getJwt();
        if (jwt != null) {
          request.getHeaders().set(HttpHeaders.AUTHORIZATION, "Bearer " + jwt);
        }
      }

      // pass along to the next interceptor
      return execution.execute(request, body);
    }
  }

  // TODO: we can think of using an interceptor to remove the cache from the biosamples client

  @PreDestroy
  public void close() {
    // close down public client if present
    if (publicClient.isPresent()) {
      publicClient.get().close();
    }
    // close down our own thread pools
    threadPoolExecutor.shutdownNow();
    try {
      threadPoolExecutor.awaitTermination(1, TimeUnit.MINUTES);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  public Optional<Resource<Sample>> fetchSampleResource(String accession)
      throws RestClientException {
    return fetchSampleResource(accession, Optional.empty());
  }

  public Optional<Resource<Sample>> fetchSampleResource(
      String accession, Optional<List<String>> curationDomains) throws RestClientException {
    try {
      return sampleRetrievalService.fetch(accession, curationDomains).get();
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    } catch (ExecutionException e) {
      throw new RuntimeException(e.getCause());
    }
  }

  public Iterable<Resource<Sample>> fetchSampleResourceAll() throws RestClientException {
    return sampleCursorRetrievalService.fetchAll("", Collections.emptyList());
  }

  public Iterable<Resource<Sample>> fetchSampleResourceAll(String text) throws RestClientException {
    return sampleCursorRetrievalService.fetchAll(text, Collections.emptyList());
  }

  public Iterable<Resource<Sample>> fetchSampleResourceAll(Collection<Filter> filters) {
    return sampleCursorRetrievalService.fetchAll("", filters);
  }

  public Iterable<Resource<Sample>> fetchSampleResourceAll(
      String text, Collection<Filter> filters) {
    return sampleCursorRetrievalService.fetchAll(text, filters);
  }

  public Iterable<Optional<Resource<Sample>>> fetchSampleResourceAll(Iterable<String> accessions)
      throws RestClientException {
    return sampleRetrievalService.fetchAll(accessions);
  }

  /**
   * Search for samples using pagination. This method should be used for specific pagination needs.
   * When in need for all results from a search, prefer the iterator implementation.
   *
   * @param text
   * @param page
   * @param size
   * @return a paginated results of samples relative to the search term
   */
  public PagedResources<Resource<Sample>> fetchPagedSampleResource(
      String text, int page, int size) {
    return samplePageRetrievalService.search(text, Collections.emptyList(), page, size);
  }

  public PagedResources<Resource<Sample>> fetchPagedSampleResource(
      String text, Collection<Filter> filters, int page, int size) {
    return samplePageRetrievalService.search(text, filters, page, size);
  }

  @Deprecated
  public Optional<Sample> fetchSample(String accession) throws RestClientException {
    Optional<Resource<Sample>> resource = fetchSampleResource(accession);
    if (resource.isPresent()) {
      return Optional.of(resource.get().getContent());
    } else {
      return Optional.empty();
    }
  }

  /**
   * @deprecated This has been deprecated use instead {@link #persistSampleResource(Sample)} or
   *     {@link #persistSampleResource(Sample, Boolean, Boolean)}
   */
  @Deprecated
  public Sample persistSample(Sample sample) {
    return persistSampleResource(sample).getContent();
  }

  public Resource<Sample> persistSampleResource(Sample sample) {
    return persistSampleResource(sample, null, null);
  }

  public Resource<Sample> persistSampleResource(
      Sample sample, Boolean setUpdateDate, Boolean setFullDetails) {
    try {
      return persistSampleResourceAsync(sample, setUpdateDate, setFullDetails).get();
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    } catch (ExecutionException e) {
      throw new RuntimeException(e.getCause());
    }
  }

  public Future<Resource<Sample>> persistSampleResourceAsync(Sample sample) {
    return persistSampleResourceAsync(sample, null, null);
  }

  public Future<Resource<Sample>> persistSampleResourceAsync(
      Sample sample, Boolean setUpdateDate, Boolean setFullDetails) {
    // validate client-side before submission
    Collection<String> errors = sampleValidator.validate(sample);
    if (!errors.isEmpty()) {
      log.error("Sample failed validation : {}", errors);
      throw new IllegalArgumentException("Sample not valid: " + String.join(", ", errors));
    }
    return sampleSubmissionService.submitAsync(sample, setUpdateDate, setFullDetails);
  }

  public Collection<Resource<Sample>> persistSamples(Collection<Sample> samples) {
    return persistSamples(samples, null, null);
  }

  public Collection<Resource<Sample>> persistSamples(
      Collection<Sample> samples, Boolean setUpdateDate, Boolean setFullDetails) {
    List<Resource<Sample>> results = new ArrayList<>();
    List<Future<Resource<Sample>>> futures = new ArrayList<>();

    for (Sample sample : samples) {
      futures.add(persistSampleResourceAsync(sample, setUpdateDate, setFullDetails));
    }

    for (Future<Resource<Sample>> future : futures) {
      Resource<Sample> sample;
      try {
        sample = future.get();
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      } catch (ExecutionException e) {
        throw new RuntimeException(e.getCause());
      }
      results.add(sample);
    }
    return results;
  }

  public Collection<Resource<Sample>> certifySamples(Collection<Sample> samples) {
    return samples.stream()
        .map(sample -> sampleCertificationService.submit(sample, null))
        .collect(Collectors.toList());
  }

  public Iterable<Resource<Curation>> fetchCurationResourceAll() {
    return curationRetrievalService.fetchAll();
  }

  public Resource<CurationLink> persistCuration(
      String accession, Curation curation, String domain) {
    log.trace("Persisting curation " + curation + " on " + accession + " in " + domain);
    return curationSubmissionService.submit(CurationLink.build(accession, curation, domain, null));
  }

  public Iterable<Resource<CurationLink>> fetchCurationLinksOfSample(String accession) {
    return curationRetrievalService.fetchCurationLinksOfSample(accession);
  }

  public void deleteCurationLink(CurationLink content) {
    curationSubmissionService.deleteCurationLink(content.getSample(), content.getHash());
  }

  // services including JWT to utilize original submission user credentials
  public Optional<Resource<Sample>> fetchSampleResource(String accession, String jwt)
      throws RestClientException {
    return fetchSampleResource(accession, Optional.empty(), jwt);
  }

  public Optional<Resource<Sample>> fetchSampleResource(
      String accession, Optional<List<String>> curationDomains, String jwt)
      throws RestClientException {
    try {
      return sampleRetrievalService.fetch(accession, curationDomains, jwt).get();
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    } catch (ExecutionException e) {
      throw new RuntimeException(e.getCause());
    }
  }

  public Optional<Resource<Sample>> fetchSampleResource(
      String accession,
      Optional<List<String>> curationDomains,
      String jwt,
      StaticViewWrapper.StaticView staticView)
      throws RestClientException {
    try {
      return sampleRetrievalService.fetch(accession, curationDomains, jwt, staticView).get();
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    } catch (ExecutionException e) {
      throw new RuntimeException(e.getCause());
    }
  }

  public Iterable<Optional<Resource<Sample>>> fetchSampleResourceAll(
      Iterable<String> accessions, String jwt) throws RestClientException {
    return sampleRetrievalService.fetchAll(accessions, jwt);
  }

  public Iterable<Resource<Sample>> fetchSampleResourceAll(
      String text, Collection<Filter> filters, String jwt) {
    return sampleCursorRetrievalService.fetchAll(text, filters, jwt);
  }

  public Iterable<Resource<Sample>> fetchSampleResourceAll(
      String text,
      Collection<Filter> filters,
      String jwt,
      StaticViewWrapper.StaticView staticView) {
    return sampleCursorRetrievalService.fetchAll(text, filters, jwt, staticView);
  }

  public PagedResources<Resource<Sample>> fetchPagedSampleResource(
      String text, Collection<Filter> filters, int page, int size, String jwt) {
    return samplePageRetrievalService.search(text, filters, page, size, jwt);
  }

  public Resource<Sample> persistSampleResource(Sample sample, String jwt) {
    try {
      return persistSampleResourceAsync(sample, jwt, false).get();
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    } catch (Exception e) {
      throw new RuntimeException(e.getCause());
    }
  }

  public Future<Resource<Sample>> persistSampleResourceAsync(
      Sample sample, String jwt, boolean setFullDetails) {
    Collection<String> errors = sampleValidator.validate(sample);
    if (!errors.isEmpty()) {
      log.error("Sample failed validation : {}", errors);
      throw new IllegalArgumentException("Sample not valid: " + String.join(", ", errors));
    }
    return sampleSubmissionService.submitAsync(sample, jwt, setFullDetails);
  }

  public Iterable<Resource<Curation>> fetchCurationResourceAll(String jwt) {
    return curationRetrievalService.fetchAll(jwt);
  }

  public Resource<CurationLink> persistCuration(
      String accession, Curation curation, String domain, String jwt) {
    log.trace("Persisting curation {} on {} in {}", curation, accession, domain);
    return curationSubmissionService.persistCuration(
        CurationLink.build(accession, curation, domain, null), jwt);
  }

  public Iterable<Resource<CurationLink>> fetchCurationLinksOfSample(String accession, String jwt) {
    return curationRetrievalService.fetchCurationLinksOfSample(accession, jwt);
  }

  public void deleteCurationLink(CurationLink content, String jwt) {
    curationSubmissionService.deleteCurationLink(content.getSample(), content.getHash(), jwt);
  }

  public Future<Resource<Sample>> persistSampleGroup(Sample sample, String jwt) {
    // validate client-side before submission
    Collection<String> errors = sampleValidator.validate(sample);
    if (!errors.isEmpty()) {
      log.error("Sample failed validation : {}", errors);
      throw new IllegalArgumentException("Sample not valid: " + String.join(", ", errors));
    }
    return sampleGroupSubmissionService.submitAsync(sample, jwt);
  }
}
