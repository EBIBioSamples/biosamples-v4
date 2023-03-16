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
package uk.ac.ebi.biosamples.client.utils;

import java.net.URI;
import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.*;
import org.springframework.hateoas.client.Hop;
import org.springframework.hateoas.client.Traverson;
import org.springframework.hateoas.client.Traverson.TraversalBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestOperations;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriTemplate;

public class IterableResourceFetchAll<T> implements Iterable<EntityModel<T>> {
  private final Traverson traverson;
  private final RestOperations restOperations;
  private final Hop[] hops;
  private final ParameterizedTypeReference<PagedModel<EntityModel<T>>> parameterizedTypeReference;
  private final MultiValueMap<String, String> params;
  private final ExecutorService executor;
  private final String jwt;

  public IterableResourceFetchAll(
      final ExecutorService executor,
      final Traverson traverson,
      final RestOperations restOperations,
      final ParameterizedTypeReference<PagedModel<EntityModel<T>>> parameterizedTypeReference,
      final String jwt,
      final MultiValueMap<String, String> params,
      final String... rels) {
    this(
        executor,
        traverson,
        restOperations,
        parameterizedTypeReference,
        jwt,
        params,
        Arrays.stream(rels).map(Hop::rel).toArray(Hop[]::new));
  }

  public IterableResourceFetchAll(
      final ExecutorService executor,
      final Traverson traverson,
      final RestOperations restOperations,
      final ParameterizedTypeReference<PagedModel<EntityModel<T>>> parameterizedTypeReference,
      final String jwt,
      final MultiValueMap<String, String> params,
      final Hop... hops) {
    this.executor = executor;
    this.traverson = traverson;
    this.restOperations = restOperations;
    this.hops = hops;
    this.parameterizedTypeReference = parameterizedTypeReference;
    this.params = params;
    this.jwt = jwt;
  }

  @Override
  public Iterator<EntityModel<T>> iterator() {

    TraversalBuilder traversonBuilder = null;
    for (final Hop hop : hops) {
      if (traversonBuilder == null) {
        traversonBuilder = traverson.follow(hop);
      } else {
        traversonBuilder.follow(hop);
      }
    }

    // get the first page
    final URI uri =
        UriComponentsBuilder.fromHttpUrl(traversonBuilder.asLink().getHref())
            .queryParams(params)
            .build()
            .toUri();

    final RequestEntity<Void> requestEntity =
        IteratorResourceFetchAll.NextPageCallable.buildRequestEntity(jwt, uri);

    final ResponseEntity<PagedModel<EntityModel<T>>> responseEntity =
        restOperations.exchange(requestEntity, parameterizedTypeReference);
    return new IteratorResourceFetchAll<>(
        Objects.requireNonNull(responseEntity.getBody()),
        restOperations,
        parameterizedTypeReference,
        executor,
        jwt);
  }

  private static class IteratorResourceFetchAll<U> implements Iterator<EntityModel<U>> {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final RestOperations restOperations;
    private final ExecutorService executor;
    private final ParameterizedTypeReference<PagedModel<EntityModel<U>>> parameterizedTypeReference;
    private PagedModel<EntityModel<U>> page;
    private Iterator<EntityModel<U>> pageIterator;
    private Future<PagedModel<EntityModel<U>>> nextPageFuture;
    private final String jwt;

    IteratorResourceFetchAll(
        final PagedModel<EntityModel<U>> page,
        final RestOperations restOperations,
        final ParameterizedTypeReference<PagedModel<EntityModel<U>>> parameterizedTypeReference,
        final ExecutorService executor,
        final String jwt) {

      this.page = page;
      pageIterator = page.iterator();
      this.restOperations = restOperations;
      this.executor = executor;
      this.parameterizedTypeReference = parameterizedTypeReference;
      this.jwt = jwt;
    }

    @Override
    public synchronized boolean hasNext() {
      // pre-emptively grab the next page as a future
      if (nextPageFuture == null && page.hasLink(IanaLinkRelations.NEXT)) {

        final Link nextLink = page.getLink(IanaLinkRelations.NEXT).get();

        final URI uri;
        if (nextLink.isTemplated()) {
          final UriTemplate uriTemplate = new UriTemplate(nextLink.getHref());
          uri = uriTemplate.expand();
        } else {
          uri = URI.create(nextLink.getHref());
        }
        log.trace("getting next page uri " + uri);

        nextPageFuture =
            executor.submit(
                new NextPageCallable<>(restOperations, parameterizedTypeReference, uri, jwt));
      }

      if (pageIterator.hasNext()) {
        return true;
      }
      // at the end of this page, move to next
      if (nextPageFuture != null) {
        try {
          page = nextPageFuture.get();
          nextPageFuture = null;
        } catch (final InterruptedException | ExecutionException e) {
          throw new RuntimeException(e);
        }
        pageIterator = page.iterator();
        return hasNext();
      }
      return false;
    }

    @Override
    public EntityModel<U> next() {
      if (pageIterator.hasNext()) {
        return pageIterator.next();
      }

      // at the end of this page, move to next
      if (nextPageFuture != null) {
        try {
          page = nextPageFuture.get();
          pageIterator = page.iterator();
          nextPageFuture = null;
        } catch (final InterruptedException | ExecutionException e) {
          throw new RuntimeException(e);
        }
        if (pageIterator.hasNext()) {
          return pageIterator.next();
        }
      }
      // no more in this iterator and no more pages, so end
      throw new NoSuchElementException();
    }

    private static class NextPageCallable<V> implements Callable<PagedModel<EntityModel<V>>> {
      private final RestOperations restOperations;
      private final URI uri;
      private final ParameterizedTypeReference<PagedModel<EntityModel<V>>>
          parameterizedTypeReference;
      private final String jwt;

      NextPageCallable(
          final RestOperations restOperations,
          final ParameterizedTypeReference<PagedModel<EntityModel<V>>> parameterizedTypeReference,
          final URI uri,
          final String jwt) {
        this.restOperations = restOperations;
        this.uri = uri;
        this.parameterizedTypeReference = parameterizedTypeReference;
        this.jwt = jwt;
      }

      @Override
      public PagedModel<EntityModel<V>> call() {
        final RequestEntity<Void> requestEntity = buildRequestEntity(jwt, uri);
        final ResponseEntity<PagedModel<EntityModel<V>>> responseEntity =
            restOperations.exchange(requestEntity, parameterizedTypeReference);

        return responseEntity.getBody();
      }

      private static RequestEntity<Void> buildRequestEntity(final String jwt, final URI uri) {
        final MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add(HttpHeaders.CONTENT_TYPE, MediaTypes.HAL_JSON.toString());

        if (jwt != null) {
          headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + jwt);
        }

        return new RequestEntity<>(headers, HttpMethod.GET, uri);
      }
    }
  }
}
