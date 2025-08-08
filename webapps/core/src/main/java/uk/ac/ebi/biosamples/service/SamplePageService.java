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
package uk.ac.ebi.biosamples.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.core.model.Sample;
import uk.ac.ebi.biosamples.core.model.filter.Filter;
import uk.ac.ebi.biosamples.mongo.model.MongoCurationLink;
import uk.ac.ebi.biosamples.mongo.model.MongoSample;
import uk.ac.ebi.biosamples.mongo.repository.MongoCurationLinkRepository;
import uk.ac.ebi.biosamples.mongo.repository.MongoSampleRepository;
import uk.ac.ebi.biosamples.mongo.service.MongoSampleToSampleConverter;
import uk.ac.ebi.biosamples.mongo.service.SampleReadService;
import uk.ac.ebi.biosamples.service.search.SearchService;
import uk.ac.ebi.biosamples.solr.repo.CursorArrayList;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

/**
 * Service layer business logic for centralising repository access and conversions between different
 * controller. Use this instead of linking to repositories directly.
 *
 * @author faulcon
 */
@Service
@Slf4j
public class SamplePageService {
  private final MongoSampleRepository mongoSampleRepository;
  private final MongoCurationLinkRepository mongoCurationLinkRepository;
  private final MongoSampleToSampleConverter mongoSampleToSampleConverter;
  private final SampleReadService sampleService;
  private final SearchService searchService;

  public SamplePageService(MongoSampleRepository mongoSampleRepository,
                           MongoCurationLinkRepository mongoCurationLinkRepository,
                           MongoSampleToSampleConverter mongoSampleToSampleConverter,
                           SampleReadService sampleService,
                           @Qualifier("elasticSearchService") SearchService searchService) {
    this.mongoSampleRepository = mongoSampleRepository;
    this.mongoCurationLinkRepository = mongoCurationLinkRepository;
    this.mongoSampleToSampleConverter = mongoSampleToSampleConverter;
    this.sampleService = sampleService;
    this.searchService = searchService;
  }

  public Page<Sample> getSamplesOfExternalReference(final String urlHash, final Pageable pageable) {
    final Page<MongoSample> pageMongoSample =
        mongoSampleRepository.findByExternalReferences_Hash(urlHash, pageable);
    // convert them into a state to return
    final Page<Sample> pageSample = pageMongoSample.map(mongoSampleToSampleConverter);
    return pageSample;
  }

  public Page<Sample> getSamplesOfCuration(final String hash, final Pageable pageable) {
    final Page<MongoCurationLink> accession =
        mongoCurationLinkRepository.findByCurationHash(hash, pageable);
    // stream process each into a sample
    final Page<Sample> pageSample =
        accession.map(mcl -> sampleService.fetch(mcl.getSample(), true).get());
    return pageSample;
  }

  public Page<Sample> getSamplesByText(
      final String text,
      final Collection<Filter> filters,
      final String webinSubmissionAccountId,
      final Pageable pageable,
      final boolean applyCurations) {
    long startTime = System.nanoTime();
    final Page<String> accessionPage =
        searchService.searchForAccessions(text, new HashSet<>(filters), webinSubmissionAccountId, pageable);
    long endTime = System.nanoTime();
    log.trace("Got search page in {}ms", (endTime - startTime) / 1000000);

    startTime = System.nanoTime();
    final Page<Future<Optional<Sample>>> pageFutureSample;
    pageFutureSample = accessionPage.map(a -> sampleService.fetchAsync(a, applyCurations));

    final Page<Sample> pageSample =
        pageFutureSample.map(
            ss -> {
              try {
                if (ss.get().isPresent()) {
                  return ss.get().get();
                } else {
                  return null;
                }
              } catch (final InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
              }
            });
    endTime = System.nanoTime();
    log.trace("Got mongo page content in {}ms", (endTime - startTime) / 1000000);
    return pageSample;
  }

  public CursorArrayList<Sample> getSamplesByText(
      final String text,
      final Collection<Filter> filters,
      final String webinSubmissionAccountId,
      String cursorMark,
      int size,
      final boolean applyCurations) {
    cursorMark = validateCursor(cursorMark);
    size = validatePageSize(size);

    final CursorArrayList<String> cursorAccessionList =
        searchService.searchForAccessions(text, new HashSet<>(filters), webinSubmissionAccountId, cursorMark, size);
    final List<Future<Optional<Sample>>> listFutureSample;

    listFutureSample =
        cursorAccessionList.stream()
            .map(a -> sampleService.fetchAsync(a, applyCurations))
            .collect(Collectors.toList());

    final List<Sample> listSample = collectSampleFutures(listFutureSample);

    return new CursorArrayList<>(listSample, cursorAccessionList.getNextCursorMark());
  }

  private List<Sample> collectSampleFutures(final List<Future<Optional<Sample>>> listFutureSample) {
    return listFutureSample.stream()
        .map(
            ss -> {
              try {
                return ss.get().get();
              } catch (final InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
              }
            })
        .collect(Collectors.toList());
  }

  private String validateCursor(String cursorMark) {
    if (cursorMark == null || cursorMark.trim().isEmpty()) {
      cursorMark = "*";
    }

    return cursorMark;
  }

  private int validatePageSize(int pageSize) {
    if (pageSize > 1000) {
      pageSize = 1000;
    }

    if (pageSize < 1) {
      pageSize = 1;
    }

    return pageSize;
  }
}
