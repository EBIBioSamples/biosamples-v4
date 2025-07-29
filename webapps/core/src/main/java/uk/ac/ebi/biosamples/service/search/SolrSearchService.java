package uk.ac.ebi.biosamples.service.search;

import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.core.model.filter.Filter;
import uk.ac.ebi.biosamples.solr.model.SolrSample;
import uk.ac.ebi.biosamples.solr.repo.CursorArrayList;
import uk.ac.ebi.biosamples.solr.service.SolrSampleService;

import java.util.List;
import java.util.Set;

@Service("solrSearchService")
@RequiredArgsConstructor
@Slf4j
public class SolrSearchService implements SearchService {
  private final SolrSampleService solrSampleService;

  @Override
  @Timed("biosamples.search.page.solr")
  public Page<String> searchForAccessions(String searchTerm, Set<Filter> filters, String webinId, Pageable pageable) {
    return solrSampleService.fetchSolrSampleByText(searchTerm, filters, webinId, pageable)
        .map(SolrSample::getAccession);
  }

  @Override
  @Timed("biosamples.search.cursor.solr")
  public CursorArrayList<String> searchForAccessions(String searchTerm, Set<Filter> filters, String webinId, String cursor, int size) {
    CursorArrayList<SolrSample> samples = solrSampleService.fetchSolrSampleByText(searchTerm, filters, webinId, cursor, size);
    List<String> accessions = samples
        .stream()
        .map(SolrSample::getAccession)
        .toList();
    return new CursorArrayList<>(accessions, samples.getNextCursorMark());
  }
}
