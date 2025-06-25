package uk.ac.ebi.biosamples.service.search;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import uk.ac.ebi.biosamples.model.filter.Filter;
import uk.ac.ebi.biosamples.search.grpc.*;
import uk.ac.ebi.biosamples.solr.model.SolrSample;
import uk.ac.ebi.biosamples.solr.service.SolrSampleService;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class ESSearchService implements SearchService {
  private final SolrSampleService solrSampleService;

  @Override
  public List<String> searchForAccessions(String searchTerm, Set<Filter> filters, String webinId, Pageable pageable) {

    ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 9090).usePlaintext().build();
    SearchGrpc.SearchBlockingStub stub = SearchGrpc.newBlockingStub(channel);
    SearchResponse response = null;
    try {
      response = stub.searchSamples(SearchRequest.newBuilder().setText(searchTerm).build());
    } catch (StatusRuntimeException e) {
      log.warn("Failed to fetch samples from remote server", e);
    }

    return response.getAccessionsList();
  }

  @Override
  public List<String> searchForAccessions(String searchTerm, Set<Filter> filters, String webinId, String cursor, int size) {
    return solrSampleService.fetchSolrSampleByText(searchTerm, filters, webinId, cursor, size)
        .stream()
        .map(SolrSample::getAccession)
        .toList();
  }
}
