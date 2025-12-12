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
package uk.ac.ebi.biosamples.service.search;

import com.google.protobuf.Timestamp;
import com.google.protobuf.util.Timestamps;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import io.micrometer.core.annotation.Timed;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import uk.ac.ebi.biosamples.BioSamplesProperties;
import uk.ac.ebi.biosamples.core.model.filter.Filter;
import uk.ac.ebi.biosamples.search.grpc.*;
import uk.ac.ebi.biosamples.solr.repo.CursorArrayList;

@Service("elasticSearchService")
@RequiredArgsConstructor
@Slf4j
public class ElasticSearchService implements SearchService {
  private final BioSamplesProperties bioSamplesProperties;

  @Override
  @Timed("biosamples.search.page.elastic")
  public Page<String> searchForAccessions(
      String searchTerm, Set<Filter> filters, String webinId, Pageable pageable) {
    ManagedChannel channel =
        ManagedChannelBuilder.forAddress(
                bioSamplesProperties.getBiosamplesSearchHost(),
                bioSamplesProperties.getBiosamplesSearchPort())
            .usePlaintext()
            .build();
    SearchGrpc.SearchBlockingStub stub = SearchGrpc.newBlockingStub(channel);
    SearchResponse response;
    try {
      SearchRequest.Builder builder = SearchRequest.newBuilder();
      if (StringUtils.hasText(searchTerm)) {
        builder.setText(searchTerm);
      }
      builder.addAllFilters(SearchFilterMapper.getSearchFilters(filters, webinId));
      builder.setSize(pageable.getPageSize());
      builder.setNumber(pageable.getPageNumber());
      builder.addAllSort(pageable.getSort().stream().map(Sort.Order::toString).toList());

      response = stub.searchSamples(builder.build());
    } catch (StatusRuntimeException e) {
      log.error("Failed to fetch samples from remote server", e);
      throw new RuntimeException("Failed to fetch samples from remote server", e);
    } finally {
      channel.shutdown();
    }

    List<String> accessions = response.getAccessionsList();
    Sort sort =
        Sort.by(
            response.getSortList().stream()
                .map(s -> new Sort.Order(Sort.Direction.ASC, s))
                .toList());
    PageRequest page = PageRequest.of(response.getNumber(), response.getSize(), sort);
    long totalElements = response.getTotalElements();
    SearchAfter searchAfter = response.getSearchAfter();

    return new SearchAfterPage<>(
        accessions, page, totalElements, searchAfter.getUpdate(), searchAfter.getAccession());
  }

  @Override
  @Timed("biosamples.search.cursor.elastic")
  public CursorArrayList<String> searchForAccessions(
      String searchTerm, Set<Filter> filters, String webinId, String cursor, int size) {
    SearchAfter searchAfter = null;
    String[] cursorParts = cursor.split(",");
    if (cursorParts.length == 2) {
      Instant update = Instant.parse(cursorParts[0].trim());
      String accession = cursorParts[1].trim();
      searchAfter =
          SearchAfter.newBuilder()
              .setUpdate(
                  Timestamp.newBuilder()
                      .setSeconds(update.getEpochSecond())
                      .setNanos(update.getNano())
                      .build())
              .setAccession(accession)
              .build();
    }

    ManagedChannel channel =
        ManagedChannelBuilder.forAddress(
                bioSamplesProperties.getBiosamplesSearchHost(),
                bioSamplesProperties.getBiosamplesSearchPort())
            .usePlaintext()
            .build();
    SearchGrpc.SearchBlockingStub stub = SearchGrpc.newBlockingStub(channel);
    SearchResponse response;
    try {
      SearchRequest.Builder builder = SearchRequest.newBuilder();
      if (StringUtils.hasText(searchTerm)) {
        builder.setText(searchTerm);
      }
      builder.addAllFilters(SearchFilterMapper.getSearchFilters(filters, webinId));
      builder.setSize(size);
      if (searchAfter != null) {
        builder.setSearchAfter(searchAfter);
      }

      response = stub.searchSamples(builder.build());
    } catch (StatusRuntimeException e) {
      log.error("Failed to fetch samples from remote server", e);
      throw new RuntimeException("Failed to fetch samples from remote server", e);
    } finally {
      channel.shutdown();
    }

    List<String> accessions = response.getAccessionsList();
    SearchAfter newSearchAfter = response.getSearchAfter();

    if (StringUtils.hasText(newSearchAfter.getAccession())) {
      cursor =
          Timestamps.toString(newSearchAfter.getUpdate()) + "," + newSearchAfter.getAccession();
    }

    return new CursorArrayList<>(accessions, cursor);
  }

  /*public OutputStream searchForAccessionsStream(String searchTerm, Set<Filter> filters, String webinId, String cursor, int size) {
    SearchAfter searchAfter = null;
    String[] cursorParts = cursor.split(",");
    if (cursorParts.length == 2) {
      Instant update = Instant.parse(cursorParts[0].trim());
      String accession = cursorParts[1].trim();
      searchAfter = SearchAfter.newBuilder()
          .setUpdate(Timestamp.newBuilder().setSeconds(update.getEpochSecond()).setNanos(update.getNano()).build())
          .setAccession(accession).build();
    }

    ManagedChannel channel = ManagedChannelBuilder.forAddress(bioSamplesProperties.getBiosamplesSearchHost(), 9090).usePlaintext().build();
    SearchGrpc.SearchBlockingStub stub = SearchGrpc.newBlockingStub(channel);
    Iterator<StreamResponse> response;
    try {
      StreamRequest.Builder builder = StreamRequest.newBuilder();
      if (StringUtils.hasText(searchTerm)) {
        builder.setText(searchTerm);
      }
      builder.addAllFilters(SearchFilterMapper.getSearchFilters(filters, webinId));
      if (searchAfter != null) {
        builder.setSearchAfter(searchAfter);
      }

      response = stub.streamSamples(builder.build());
    } catch (StatusRuntimeException e) {
      log.warn("Failed to fetch samples from remote server", e);
      throw new RuntimeException("Failed to fetch samples from remote server", e);
    } finally {
      channel.shutdown();
    }

    List<String> accessionList = new ArrayList<>();
    while (response.hasNext()) {
      StreamResponse streamResponse = response.next();
      searchAfter = streamResponse.getSearchAfter();
      accessionList.add(streamResponse.getAccession());
      cursor = Timestamps.toString(searchAfter.getUpdate()) + "," + searchAfter.getAccession();
    }

    return new CursorArrayList<>(accessionList, cursor);
  }*/
}
