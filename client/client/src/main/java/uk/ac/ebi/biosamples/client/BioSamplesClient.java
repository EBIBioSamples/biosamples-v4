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
import java.util.*;
import javax.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.client.Traverson;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import uk.ac.ebi.biosamples.BioSamplesProperties;
import uk.ac.ebi.biosamples.client.service.*;
import uk.ac.ebi.biosamples.model.Curation;
import uk.ac.ebi.biosamples.model.CurationLink;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.filter.Filter;
import uk.ac.ebi.biosamples.model.structured.StructuredData;
import uk.ac.ebi.biosamples.service.SampleValidator;

/**
 * This is the primary class for interacting with BioSamples.
 *
 * @author faulcon
 */
public class BioSamplesClient implements AutoCloseable {
  private final Logger log = LoggerFactory.getLogger(getClass());
  private final SampleRetrievalService sampleRetrievalService;
  private final SampleRetrievalServiceV2 sampleRetrievalServiceV2;
  private final SamplePageRetrievalService samplePageRetrievalService;
  private final SampleCursorRetrievalService sampleCursorRetrievalService;
  private final SampleSubmissionService sampleSubmissionService;
  private final SampleSubmissionServiceV2 sampleSubmissionServiceV2;
  private final CurationRetrievalService curationRetrievalService;
  private final CurationSubmissionService curationSubmissionService;
  private final StructuredDataSubmissionService structuredDataSubmissionService;
  private final SampleValidator sampleValidator;
  private final Optional<BioSamplesClient> publicClient;

  /**
   * Constructs a BioSamplesClient with the given parameters.
   *
   * @param uri the URI for BioSamples
   * @param uriV2 the URI for BioSamples V2
   * @param restTemplateBuilder the RestTemplateBuilder
   * @param sampleValidator the SampleValidator
   * @param clientService the ClientService
   * @param bioSamplesProperties the BioSamplesProperties
   */
  public BioSamplesClient(
      final URI uri,
      URI uriV2,
      final RestTemplateBuilder restTemplateBuilder,
      final SampleValidator sampleValidator,
      final ClientService clientService,
      final BioSamplesProperties bioSamplesProperties) {
    if (uriV2 == null) {
      uriV2 = UriComponentsBuilder.fromUri(URI.create(uri + "/v2")).build().toUri();
    }

    final RestTemplate restOperations = restTemplateBuilder.build();

    if (clientService != null) {
      if (clientService instanceof AapClientService) {
        log.trace("Adding BsdClientHttpRequestInterceptor");
        restOperations.getInterceptors().add(new BsdClientHttpRequestInterceptor(clientService));
      } else if (clientService instanceof WebinAuthClientService) {
        log.trace("Adding WebinClientHttpRequestInterceptor");
        restOperations.getInterceptors().add(new BsdClientHttpRequestInterceptor(clientService));
      } else {
        log.trace("No ClientService available");
      }
    }

    final Traverson traverson = new Traverson(uri, MediaTypes.HAL_JSON);
    traverson.setRestOperations(restOperations);

    sampleRetrievalService = new SampleRetrievalService(restOperations, traverson);
    samplePageRetrievalService = new SamplePageRetrievalService(restOperations, traverson);
    sampleCursorRetrievalService =
        new SampleCursorRetrievalService(
            restOperations, traverson, bioSamplesProperties.getBiosamplesClientPagesize());

    sampleSubmissionService = new SampleSubmissionService(restOperations, traverson);

    sampleSubmissionServiceV2 = new SampleSubmissionServiceV2(restOperations, uriV2);

    sampleRetrievalServiceV2 = new SampleRetrievalServiceV2(restOperations, uriV2);

    curationRetrievalService =
        new CurationRetrievalService(
            restOperations, traverson, bioSamplesProperties.getBiosamplesClientPagesize());

    /*TODO: In CurationSubmissionService and StructuredDataSubmissionService webin auth is handled more elegantly, replicate it in all other services*/
    curationSubmissionService = new CurationSubmissionService(restOperations, traverson);

    structuredDataSubmissionService =
        new StructuredDataSubmissionService(restOperations, traverson);

    this.sampleValidator = sampleValidator;

    if (clientService == null) {
      publicClient = Optional.empty();
    } else {
      publicClient =
          Optional.of(
              new BioSamplesClient(
                  uri, uriV2, restTemplateBuilder, sampleValidator, null, bioSamplesProperties));
    }
  }

  /**
   * Gets the public client.
   *
   * @return the public client
   */
  public Optional<BioSamplesClient> getPublicClient() {
    return publicClient;
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

  /**
   * Fetches a sample resource by accession using BioSamples.
   *
   * @param accession the accession of the sample
   * @return the optional sample resource
   * @throws RestClientException if there is an error while fetching the sample
   */
  public Optional<EntityModel<Sample>> fetchSampleResource(final String accession)
      throws RestClientException {
    return fetchSampleResource(accession, Optional.empty());
  }

  /**
   * Fetches a sample resource by accession using BioSamples with optional curation domains.
   *
   * @param accession the accession of the sample
   * @param curationDomains the optional list of curation domains
   * @return the optional sample resource
   * @throws RestClientException if there is an error while fetching the sample
   */
  public Optional<EntityModel<Sample>> fetchSampleResource(
      final String accession, final Optional<List<String>> curationDomains)
      throws RestClientException {
    try {
      return sampleRetrievalService.fetch(accession, curationDomains);
    } catch (final Exception e) {
      throw new RuntimeException(e.getCause());
    }
  }

  /**
   * Fetches all sample resources using BioSamples.
   *
   * @return an iterable of sample resources
   * @throws RestClientException if there is an error while fetching samples
   */
  public Iterable<EntityModel<Sample>> fetchSampleResourceAll() throws RestClientException {
    return sampleCursorRetrievalService.fetchAll("", Collections.emptyList());
  }

  /**
   * Fetches all sample resources using BioSamples with curation inclusion.
   *
   * @param addCurations true if curations should be included
   * @return an iterable of sample resources
   * @throws RestClientException if there is an error while fetching samples
   */
  public Iterable<EntityModel<Sample>> fetchSampleResourceAll(final boolean addCurations)
      throws RestClientException {
    return sampleCursorRetrievalService.fetchAll("", Collections.emptyList(), null, addCurations);
  }

  /**
   * Fetches all sample resources using BioSamples with a text query.
   *
   * @param text the text query
   * @return an iterable of sample resources
   * @throws RestClientException if there is an error while fetching samples
   */
  public Iterable<EntityModel<Sample>> fetchSampleResourceAll(final String text)
      throws RestClientException {
    return sampleCursorRetrievalService.fetchAll(text, Collections.emptyList());
  }

  /**
   * Fetches all sample resources using BioSamples with specified filters.
   *
   * @param filters the collection of filters
   * @return an iterable of sample resources
   */
  public Iterable<EntityModel<Sample>> fetchSampleResourceAll(final Collection<Filter> filters) {
    return sampleCursorRetrievalService.fetchAll("", filters);
  }

  /**
   * Fetches all sample resources using BioSamples with a text query and specified filters.
   *
   * @param text the text query
   * @param filters the collection of filters
   * @return an iterable of sample resources
   */
  public Iterable<EntityModel<Sample>> fetchSampleResourceAll(
      final String text, final Collection<Filter> filters) {
    return sampleCursorRetrievalService.fetchAll(text, filters);
  }

  /**
   * Fetches all sample resources using BioSamples with a text query and specified filters excluding
   * curations.
   *
   * @param text the text query
   * @param filters the collection of filters
   * @return an iterable of sample resources
   */
  public Iterable<EntityModel<Sample>> fetchSampleResourceAllWithoutCuration(
      final String text, final Collection<Filter> filters) {
    return sampleCursorRetrievalService.fetchAllWithoutCurations(text, filters);
  }

  /**
   * Fetches sample resources by accessions using BioSamples.
   *
   * @param accessions the iterable of accessions
   * @return an iterable of optional sample resources
   * @throws RestClientException if there is an error while fetching samples
   */
  public Iterable<Optional<EntityModel<Sample>>> fetchSampleResourceAll(
      final Iterable<String> accessions) throws RestClientException {
    return sampleRetrievalService.fetchAll(accessions);
  }

  /**
   * Searches for samples using pagination. This method should be used for specific pagination
   * needs. When in need for all results from a search, prefer the iterator implementation.
   *
   * @param text the text query
   * @param page the page number
   * @param size the page size
   * @return a paged model of sample resources
   */
  public PagedModel<EntityModel<Sample>> fetchPagedSampleResource(
      final String text, final int page, final int size) {
    return samplePageRetrievalService.search(text, Collections.emptyList(), page, size);
  }

  /**
   * Searches for samples using pagination with specified filters. This method should be used for
   * specific pagination needs. When in need for all results from a search, prefer the iterator
   * implementation.
   *
   * @param text the text query
   * @param filters the collection of filters
   * @param page the page number
   * @param size the page size
   * @return a paged model of sample resources
   */
  public PagedModel<EntityModel<Sample>> fetchPagedSampleResource(
      final String text, final Collection<Filter> filters, final int page, final int size) {
    return samplePageRetrievalService.search(text, filters, page, size);
  }

  /**
   * Deprecated method: Fetches a sample by accession using BioSamples.
   *
   * @param accession the accession of the sample
   * @return the optional sample
   * @throws RestClientException if there is an error while fetching the sample
   * @deprecated This method has been deprecated. Use {@link #fetchSampleResource(String)} or {@link
   *     #fetchSampleResource(String, Optional)} instead.
   */
  @Deprecated
  public Optional<Sample> fetchSample(final String accession) throws RestClientException {
    final Optional<EntityModel<Sample>> resource = fetchSampleResource(accession);
    return resource.map(EntityModel::getContent);
  }

  /**
   * Deprecated method: Persists a sample using BioSamples.
   *
   * @param sample the sample to persist
   * @return the persisted sample
   * @deprecated This method has been deprecated. Use {@link #persistSampleResource(Sample)}
   *     instead.
   */
  @Deprecated
  public Sample persistSample(final Sample sample) {
    return persistSampleResource(sample).getContent();
  }

  /**
   * Persists multiple sample resources using BioSamples V2.
   *
   * @param samples the list of samples to persist
   * @return the list of persisted samples
   */
  public List<Sample> persistSampleResourceV2(final List<Sample> samples) {
    return sampleSubmissionServiceV2.submit(samples);
  }

  /**
   * Persists multiple sample resources using BioSamples V2 with JWT authentication.
   *
   * @param samples the list of samples to persist
   * @param jwt the JWT token
   * @return the list of persisted samples
   */
  public List<Sample> persistSampleResourceV2(final List<Sample> samples, final String jwt) {
    return sampleSubmissionServiceV2.submit(samples, jwt);
  }

  /**
   * Accessions multiple sample resources using BioSamples V2.
   *
   * @param samples the list of samples to accession
   * @return a map of original accession to new accession
   */
  public Map<String, String> bulkAccessionV2(final List<Sample> samples) {
    return sampleSubmissionServiceV2.accession(samples);
  }

  /**
   * Accessions multiple sample resources asynchronously using BioSamples V2 with JWT
   * authentication.
   *
   * @param samples the list of samples to accession
   * @param jwt the JWT token
   * @return a map of original accession to new accession
   */
  public Map<String, String> bulkAccessionV2(final List<Sample> samples, final String jwt) {
    return sampleSubmissionServiceV2.accession(samples, jwt);
  }

  public EntityModel<Sample> persistSampleResource(final Sample sample) {
    // validate client-side before submission
    final Collection<String> errors = sampleValidator.validate(sample);

    if (!errors.isEmpty()) {
      log.error("Sample failed validation : {}", errors);
      throw new IllegalArgumentException("Sample not valid: " + String.join(", ", errors));
    }

    return sampleSubmissionService.submit(sample);
  }

  public Collection<EntityModel<Sample>> persistSamples(final Collection<Sample> samples) {
    final List<EntityModel<Sample>> results = new ArrayList<>();

    for (final Sample sample : samples) {
      results.add(persistSampleResource(sample));
    }

    return results;
  }

  public Iterable<EntityModel<Curation>> fetchCurationResourceAll() {
    return curationRetrievalService.fetchAll();
  }

  public EntityModel<CurationLink> persistCuration(
      final String accession,
      final Curation curation,
      final String webinIdOrDomain,
      final boolean isWebin) {
    log.trace("Persisting curation " + curation + " on " + accession + " using " + webinIdOrDomain);

    return curationSubmissionService.submit(
        buildCurationLink(accession, curation, webinIdOrDomain, isWebin));
  }

  public Iterable<EntityModel<CurationLink>> fetchCurationLinksOfSample(final String accession) {
    return curationRetrievalService.fetchCurationLinksOfSample(accession);
  }

  public void deleteCurationLink(final CurationLink content) {
    curationSubmissionService.deleteCurationLink(content.getSample(), content.getHash());
  }

  // services including JWT to utilize original submission user credentials
  public Optional<EntityModel<Sample>> fetchSampleResource(final String accession, final String jwt)
      throws RestClientException {
    return fetchSampleResource(accession, Optional.empty(), jwt);
  }

  public Optional<EntityModel<Sample>> fetchSampleResource(
      final String accession, final Optional<List<String>> curationDomains, final String jwt)
      throws RestClientException {
    try {
      return sampleRetrievalService.fetch(accession, curationDomains, jwt);
    } catch (final Exception e) {
      throw new RuntimeException(e.getCause());
    }
  }

  public Iterable<Optional<EntityModel<Sample>>> fetchSampleResourceAll(
      final Iterable<String> accessions, final String jwt) throws RestClientException {
    return sampleRetrievalService.fetchAll(accessions, jwt);
  }

  public Iterable<EntityModel<Sample>> fetchSampleResourceAll(
      final String text, final Collection<Filter> filters, final String jwt) {
    return sampleCursorRetrievalService.fetchAll(text, filters, jwt);
  }

  public PagedModel<EntityModel<Sample>> fetchPagedSampleResource(
      final String text,
      final Collection<Filter> filters,
      final int page,
      final int size,
      final String jwt) {
    return samplePageRetrievalService.search(text, filters, page, size, jwt);
  }

  public EntityModel<Sample> persistSampleResource(final Sample sample, final String jwt) {
    final Collection<String> errors = sampleValidator.validate(sample);

    if (!errors.isEmpty()) {
      log.error("Sample failed validation : {}", errors);
      throw new IllegalArgumentException("Sample not valid: " + String.join(", ", errors));
    }

    return sampleSubmissionService.submit(sample, jwt);
  }

  public Iterable<EntityModel<Curation>> fetchCurationResourceAll(final String jwt) {
    return curationRetrievalService.fetchAll(jwt);
  }

  public EntityModel<CurationLink> persistCuration(
      final String accession,
      final Curation curation,
      final String webinIdOrDomain,
      final String jwt,
      final boolean isWebin) {
    log.trace("Persisting curation {} on {} in {}", curation, accession, webinIdOrDomain);

    final CurationLink curationLink =
        buildCurationLink(accession, curation, webinIdOrDomain, isWebin);

    return curationSubmissionService.submit(curationLink);
  }

  private CurationLink buildCurationLink(
      final String accession,
      final Curation curation,
      final String webinIdOrDomain,
      final boolean isWebin) {
    final CurationLink curationLink;

    if (isWebin) {
      log.trace(
          "Persisting curation "
              + curation
              + " on "
              + accession
              + " using WEBIN ID "
              + webinIdOrDomain);
      curationLink = CurationLink.build(accession, curation, null, webinIdOrDomain, null);
    } else {
      log.trace(
          "Persisting curation "
              + curation
              + " on "
              + accession
              + " using DOMAIN "
              + webinIdOrDomain);
      curationLink = CurationLink.build(accession, curation, webinIdOrDomain, null, null);
    }

    return curationLink;
  }

  public Iterable<EntityModel<CurationLink>> fetchCurationLinksOfSample(
      final String accession, final String jwt) {
    return curationRetrievalService.fetchCurationLinksOfSample(accession, jwt);
  }

  public void deleteCurationLink(final CurationLink content, final String jwt) {
    curationSubmissionService.deleteCurationLink(content.getSample(), content.getHash(), jwt);
  }

  public EntityModel<StructuredData> persistStructuredData(final StructuredData structuredData) {
    return structuredDataSubmissionService.persistStructuredData(structuredData, null);
  }
}
