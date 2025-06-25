package uk.ac.ebi.biosamples.service.search;

import org.springframework.data.domain.Pageable;
import uk.ac.ebi.biosamples.model.filter.Filter;

import java.util.List;
import java.util.Set;

public interface SearchService {
  List<String> searchForAccessions(String searchTerm, Set<Filter> filters, String webinId, Pageable pageable);

  List<String> searchForAccessions(String searchTerm, Set<Filter> filters, String webinId, String cursor, int size);
}
