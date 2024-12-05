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
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.client.Traverson;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestOperations;
import org.springframework.web.util.UriComponentsBuilder;
import uk.ac.ebi.biosamples.model.Sample;

public class SampleRetrievalService {
  /*TODO: check if private sample fetch work - jwt not used*/
  private final Logger log = LoggerFactory.getLogger(getClass());
  private final Traverson traverson;
  private final RestOperations restOperations;

  public SampleRetrievalService(final RestOperations restOperations, final Traverson traverson) {
    this.restOperations = restOperations;
    this.traverson = traverson;
  }

  /** This will get an existing sample from biosamples using the accession */
  public Optional<EntityModel<Sample>> fetch(final String accession, final boolean applyCurations) {
    return new SampleRetriever(accession, applyCurations).fetchSample();
  }

  public Optional<EntityModel<Sample>> fetch(
      final String accession, final boolean applyCurations, final String jwt) {
    return new SampleRetriever(accession, applyCurations, jwt).fetchSample();
  }

  private class SampleRetriever {
    private final String accession;
    private final boolean applyCurations;
    private final String jwt;

    SampleRetriever(final String accession, final boolean applyCurations) {
      this.accession = accession;
      this.applyCurations = applyCurations;
      this.jwt = null;
    }

    SampleRetriever(final String accession, final boolean applyCurations, final String jwt) {
      this.accession = accession;
      this.applyCurations = applyCurations;
      this.jwt = jwt;
    }

    public Optional<EntityModel<Sample>> fetchSample() {
      final Traverson.TraversalBuilder traversalBuilder = traverson.follow("samples");

      // Get the base URI from the traversal
      final String baseUri = traversalBuilder.asLink().getHref() + "/" + accession;

      log.info("Base URL here " + baseUri);

      // Add the query parameter
      final String uri =
          UriComponentsBuilder.fromUriString(baseUri)
              .queryParam("applyCurations", applyCurations) // Append query parameter
              .build()
              .toUriString();

      log.info("GETing " + uri);

      // Prepare headers for the request
      final MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();

      headers.add(HttpHeaders.CONTENT_TYPE, MediaTypes.HAL_JSON.toString());

      if (jwt != null) {
        headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + jwt);
      }

      // Create the request entity
      final RequestEntity<Void> requestEntity =
          new RequestEntity<>(headers, HttpMethod.GET, URI.create(uri));
      final ResponseEntity<EntityModel<Sample>> responseEntity;

      try {
        responseEntity =
            restOperations.exchange(
                requestEntity, new ParameterizedTypeReference<EntityModel<Sample>>() {});
      } catch (final HttpStatusCodeException e) {
        if (e.getStatusCode().equals(HttpStatus.FORBIDDEN)
            || e.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
          return Optional.empty();
        } else {
          throw e;
        }
      }

      log.trace("GETted " + uri);

      return Optional.ofNullable(responseEntity.getBody());
    }
  }

  public Iterable<Optional<EntityModel<Sample>>> fetchAll(final Iterable<String> accessions) {
    return new IterableResourceFetch(accessions);
  }

  public Iterable<Optional<EntityModel<Sample>>> fetchAll(
      final Iterable<String> accessions, final String jwt) {
    return new IterableResourceFetch(accessions);
  }

  private class IterableResourceFetch implements Iterable<Optional<EntityModel<Sample>>> {
    private final Iterable<String> accessions;
    private final String jwt;

    IterableResourceFetch(final Iterable<String> accessions) {
      this.accessions = accessions;
      jwt = null;
    }

    public IterableResourceFetch(final Iterable<String> accessions, final String jwt) {
      this.accessions = accessions;
      this.jwt = jwt;
    }

    @Override
    public Iterator<Optional<EntityModel<Sample>>> iterator() {
      return new IteratorResourceFetch(accessions.iterator());
    }

    private class IteratorResourceFetch implements Iterator<Optional<EntityModel<Sample>>> {
      private final Iterator<String> accessions;
      private final Queue<Optional<EntityModel<Sample>>> queue = new LinkedList<>();

      IteratorResourceFetch(final Iterator<String> accessions) {
        this.accessions = accessions;
      }

      @Override
      public boolean hasNext() {
        if (accessions.hasNext()) {
          return true;
        } else {
          return !queue.isEmpty();
        }
      }

      @Override
      public Optional<EntityModel<Sample>> next() {
        if (!hasNext()) {
          throw new NoSuchElementException();
        }

        // fill up the queue if possible
        final int queueMaxSize = 1000;

        while (queue.size() < queueMaxSize && accessions.hasNext()) {
          log.trace("Queue size is " + queue.size());

          queue.add(fetch(accessions.next(), true, jwt));
        }

        // get the end of the queue and wait for it to finish if needed
        final Optional<EntityModel<Sample>> pollingResult = queue.poll();
        // this shouldn't happen, but best to check

        if (!pollingResult.isPresent()) {
          throw new NoSuchElementException();
        }

        return pollingResult;
      }
    }
  }
}
