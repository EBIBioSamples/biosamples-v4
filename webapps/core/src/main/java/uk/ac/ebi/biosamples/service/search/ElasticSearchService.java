package uk.ac.ebi.biosamples.service.search;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.core.model.filter.Filter;
import uk.ac.ebi.biosamples.search.grpc.*;
import uk.ac.ebi.biosamples.solr.repo.CursorArrayList;

import java.util.Iterator;
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
      response = stub.searchSamples(SearchRequest.newBuilder().setText(searchTerm).build());
    } catch (StatusRuntimeException e) {
      log.error("Failed to fetch samples from remote server", e);
      throw new RuntimeException("Failed to fetch samples from remote server", e);
    }

    response.getAccessionsList();
    return Page.empty();
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
