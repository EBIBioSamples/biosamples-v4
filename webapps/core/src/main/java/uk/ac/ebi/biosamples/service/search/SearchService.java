package uk.ac.ebi.biosamples.service.search;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import uk.ac.ebi.biosamples.model.filter.Filter;
import uk.ac.ebi.biosamples.solr.repo.CursorArrayList;

import java.util.Set;

public interface SearchService {
  Page<String> searchForAccessions(String searchTerm, Set<Filter> filters, String webinId, Pageable pageable);

  CursorArrayList<String> searchForAccessions(String searchTerm, Set<Filter> filters, String webinId, String cursor, int size);
}
