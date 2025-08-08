package uk.ac.ebi.biosamples.service.search;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import uk.ac.ebi.biosamples.core.model.filter.Filter;
import uk.ac.ebi.biosamples.search.grpc.*;
import uk.ac.ebi.biosamples.solr.repo.CursorArrayList;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

@Service("elasticSearchService")
@RequiredArgsConstructor
@Slf4j
public class ElasticSearchService implements SearchService {

  @Override
  @Timed("biosamples.search.page.elastic")
  public Page<String> searchForAccessions(String searchTerm, Set<Filter> filters, String webinId, Pageable pageable) {

    ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 9090).usePlaintext().build();
    SearchGrpc.SearchBlockingStub stub = SearchGrpc.newBlockingStub(channel);
    SearchResponse response;
    try {
      SearchRequest.Builder builder = SearchRequest.newBuilder();
      if (StringUtils.hasText(searchTerm)) {
        builder.setText(searchTerm);
      }

      builder.addAllFilters(GrpcFilterUtils.getSearchFilters(filters, webinId));

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
    Sort sort = Sort.by(response.getSortList().stream().map(s -> new Sort.Order(Sort.Direction.ASC, s)).toList());
    PageRequest page = PageRequest.of(response.getNumber(), response.getSize(), sort) ;
    long totalElements = response.getTotalElements();
    String searchAfter = response.getSearchAfter();

    return new SearchAfterPage<>(accessions, page, totalElements, searchAfter);
  }

  @Override
  @Timed("biosamples.search.cursor.elastic")
  public CursorArrayList<String> searchForAccessions(String searchTerm, Set<Filter> filters, String webinId, String cursor, int size) {
    ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 9090).usePlaintext().build();
    SearchGrpc.SearchBlockingStub stub = SearchGrpc.newBlockingStub(channel);
    Iterator<StreamResponse> response;
    try {
      response = stub.streamSamples(StreamRequest.newBuilder().setText(searchTerm).build());
    } catch (StatusRuntimeException e) {
      log.warn("Failed to fetch samples from remote server", e);
      throw new RuntimeException("Failed to fetch samples from remote server", e);
    }

    StreamResponse searchResponse = response.next();
    return new CursorArrayList<>(searchResponse.getAccession());
  }
}
