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
package uk.ac.ebi.biosamples.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.model.filter.Filter;
import uk.ac.ebi.biosamples.service.search.SearchService;
import uk.ac.ebi.biosamples.utils.LinkUtils;

import java.util.Collection;
import java.util.HashSet;

@Service
public class AccessionsService {
  private final FilterService filterService;
  private final SearchService searchService;

  public AccessionsService(FilterService filterService,
                           @Qualifier("solrSearchService") SearchService searchService) {
    this.filterService = filterService;
    this.searchService = searchService;
  }

  public Page<String> getAccessions(String text,
                                    String[] requestFilters,
                                    String webinSubmissionAccountId,
                                    Integer page,
                                    Integer size) {
    PageRequest pageable = PageRequest.of(page, size);
    String decodedText = LinkUtils.decodeText(text);
    String[] decodedFilter = LinkUtils.decodeTexts(requestFilters);
    Collection<Filter> filtersAfterDecode = filterService.getFiltersCollection(decodedFilter);

    return fetchAccessions(pageable, decodedText, filtersAfterDecode, webinSubmissionAccountId);
  }

  private Page<String> fetchAccessions(PageRequest pageable,
                                       String decodedText,
                                       Collection<Filter> filtersAfterDecode,
                                       String webinSubmissionAccountId) {
    return searchService.searchForAccessions(
        decodedText, new HashSet<>(filtersAfterDecode), webinSubmissionAccountId, pageable);
  }
}
