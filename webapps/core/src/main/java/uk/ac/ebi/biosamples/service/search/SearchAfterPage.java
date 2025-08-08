package uk.ac.ebi.biosamples.service.search;

import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

public class SearchAfterPage<T> extends PageImpl<T> {
  private final String searchAfter;

  public SearchAfterPage(List<T> content, Pageable pageable, long total, String searchAfter) {
    super(content, pageable, total);
    this.searchAfter = searchAfter;
  }

  public String getSearchAfter() {
    return searchAfter;
  }
}
