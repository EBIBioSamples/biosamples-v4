package uk.ac.ebi.biosamples.service.search;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.model.filter.Filter;
import uk.ac.ebi.biosamples.solr.model.SolrSample;
import uk.ac.ebi.biosamples.solr.service.SolrSampleService;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class SolrSearchService implements SearchService {
  private final SolrSampleService solrSampleService;

  @Override
  public List<String> searchForAccessions(String searchTerm, Set<Filter> filters, String webinId, Pageable pageable) {
    return solrSampleService.fetchSolrSampleByText(searchTerm, filters, webinId, pageable).getContent()
        .stream()
        .map(SolrSample::getAccession)
        .toList();
  }

  @Override
  public List<String> searchForAccessions(String searchTerm, Set<Filter> filters, String webinId, String cursor, int size) {
    return solrSampleService.fetchSolrSampleByText(searchTerm, filters, webinId, cursor, size)
        .stream()
        .map(SolrSample::getAccession)
        .toList();
  }
}
