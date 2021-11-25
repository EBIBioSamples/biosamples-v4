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

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.StaticViewWrapper;
import uk.ac.ebi.biosamples.model.filter.Filter;
import uk.ac.ebi.biosamples.mongo.model.MongoCurationLink;
import uk.ac.ebi.biosamples.mongo.model.MongoSample;
import uk.ac.ebi.biosamples.mongo.repo.MongoCurationLinkRepository;
import uk.ac.ebi.biosamples.mongo.repo.MongoSampleRepository;
import uk.ac.ebi.biosamples.mongo.service.MongoSampleToSampleConverter;
import uk.ac.ebi.biosamples.solr.model.SolrSample;
import uk.ac.ebi.biosamples.solr.repo.CursorArrayList;
import uk.ac.ebi.biosamples.solr.service.SolrSampleService;

/**
 * Service layer business logic for centralising repository access and conversions between different
 * controller. Use this instead of linking to repositories directly.
 *
 * @author faulcon
 */
@Service
public class SamplePageService {

  private Logger log = LoggerFactory.getLogger(getClass());

  @Autowired private MongoSampleRepository mongoSampleRepository;
  @Autowired private MongoCurationLinkRepository mongoCurationLinkRepository;

  // TODO use a ConversionService to manage all these
  @Autowired private MongoSampleToSampleConverter mongoSampleToSampleConverter;

  @Autowired private SampleReadService sampleService;

  @Autowired private SolrSampleService solrSampleService;

  public Page<Sample> getSamplesOfExternalReference(String urlHash, Pageable pageable) {
    Page<MongoSample> pageMongoSample =
        mongoSampleRepository.findByExternalReferences_Hash(urlHash, pageable);
    // convert them into a state to return
    Page<Sample> pageSample = pageMongoSample.map(mongoSampleToSampleConverter);
    return pageSample;
  }

  public Page<Sample> getSamplesOfCuration(String hash, Pageable pageable) {
    Page<MongoCurationLink> accession =
        mongoCurationLinkRepository.findByCurationHash(hash, pageable);
    // stream process each into a sample
    Page<Sample> pageSample =
        accession.map(mcl -> sampleService.fetch(mcl.getSample(), Optional.empty()).get());
    return pageSample;
  }

  public Page<Sample> getSamplesByText(
      String text,
      Collection<Filter> filters,
      Collection<String> domains,
      String webinSubmissionAccountId,
      Pageable pageable,
      String curationRepo,
      Optional<List<String>> curationDomains) {
    long startTime = System.nanoTime();
    Page<SolrSample> pageSolrSample =
        solrSampleService.fetchSolrSampleByText(
            text, filters, domains, webinSubmissionAccountId, pageable);
    long endTime = System.nanoTime();
    log.trace("Got solr page in " + ((endTime - startTime) / 1000000) + "ms");

    startTime = System.nanoTime();
    Page<Future<Optional<Sample>>> pageFutureSample;
    StaticViewWrapper.StaticView staticViews =
        StaticViewWrapper.getStaticView(domains.isEmpty() ? null : domains, curationRepo);
    pageFutureSample =
        pageSolrSample.map(
            ss -> sampleService.fetchAsync(ss.getAccession(), curationDomains, staticViews));

    Page<Sample> pageSample =
        pageFutureSample.map(
            ss -> {
              try {
                if (ss.get().isPresent()) {
                  return ss.get().get();
                } else {
                  return null;
                }
              } catch (InterruptedException e) {
                throw new RuntimeException(e);
              } catch (ExecutionException e) {
                throw new RuntimeException(e);
              }
            });
    endTime = System.nanoTime();
    log.trace("Got mongo page content in " + ((endTime - startTime) / 1000000) + "ms");
    return pageSample;
  }

  public CursorArrayList<Sample> getSamplesByText(
      String text,
      Collection<Filter> filters,
      Collection<String> domains,
      String webinSubmissionAccountId,
      String cursorMark,
      int size,
      String curationRepo,
      Optional<List<String>> curationDomains) {
    cursorMark = validateCursor(cursorMark);
    size = validatePageSize(size);

    CursorArrayList<SolrSample> cursorSolrSample =
        solrSampleService.fetchSolrSampleByText(
            text, filters, domains, webinSubmissionAccountId, cursorMark, size);

    StaticViewWrapper.StaticView staticViews =
        StaticViewWrapper.getStaticView(domains.isEmpty() ? null : domains, curationRepo);
    List<Future<Optional<Sample>>> listFutureSample;
    listFutureSample =
        cursorSolrSample.stream()
            .map(s -> sampleService.fetchAsync(s.getAccession(), curationDomains, staticViews))
            .collect(Collectors.toList());

    List<Sample> listSample = collectSampleFutures(listFutureSample);

    return new CursorArrayList<>(listSample, cursorSolrSample.getNextCursorMark());
  }

  private List<Sample> collectSampleFutures(List<Future<Optional<Sample>>> listFutureSample) {
    return listFutureSample.stream()
        .map(
            ss -> {
              try {
                return ss.get().get();
              } catch (InterruptedException e) {
                throw new RuntimeException(e);
              } catch (ExecutionException e) {
                throw new RuntimeException(e);
              }
            })
        .collect(Collectors.toList());
  }

  private String validateCursor(String cursorMark) {
    if (cursorMark == null || cursorMark.trim().length() == 0) {
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
