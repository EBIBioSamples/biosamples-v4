package uk.ac.ebi.biosamples.service.facet;

import org.springframework.data.domain.Pageable;
import uk.ac.ebi.biosamples.core.model.facet.Facet;
import uk.ac.ebi.biosamples.core.model.filter.Filter;

import java.util.List;
import java.util.Set;

public interface FacetService {
  List<Facet> getFacets(
      String searchTerm,
      Set<Filter> filters,
      String webinId,
      Pageable facetFieldPageInfo,
      Pageable facetValuesPageInfo,
      String facetField,
      List<String> facetFields);
}
