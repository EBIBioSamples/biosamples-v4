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

import java.util.Collection;
import java.util.Collections;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.model.filter.Filter;
import uk.ac.ebi.biosamples.solr.model.SolrSample;
import uk.ac.ebi.biosamples.solr.service.SolrSampleService;
import uk.ac.ebi.biosamples.utils.LinkUtils;

@Service
public class AccessionsService {
  private SolrSampleService solrSampleService;
  private FilterService filterService;

  public AccessionsService(SolrSampleService solrSampleService, FilterService filterService) {
    this.solrSampleService = solrSampleService;
    this.filterService = filterService;
  }

  public Page<String> getAccessions(
      final String text,
      final String[] requestfilters,
      final Collection<String> domains,
      final String webinSubmissionAccountId,
      final Integer page,
      final Integer size) {
    final PageRequest pageable = PageRequest.of(page, size);
    final String decodedText = LinkUtils.decodeText(text);
    final String[] decodedFilter = LinkUtils.decodeTexts(requestfilters);
    final Collection<Filter> filtersAfterDecode = filterService.getFiltersCollection(decodedFilter);

    return fetchAccessions(
        pageable, decodedText, filtersAfterDecode, domains, webinSubmissionAccountId);
  }

  private Page<String> fetchAccessions(
      final PageRequest pageable,
      final String decodedText,
      final Collection<Filter> filtersAfterDecode,
      final Collection<String> domains,
      final String webinSubmissionAccountId) {
    final Page<SolrSample> pageSolrSample =
        solrSampleService.fetchSolrSampleByText(
            decodedText,
            filtersAfterDecode,
            (domains != null && !domains.isEmpty()) ? domains : Collections.emptySet(),
            webinSubmissionAccountId,
            pageable);
    return pageSolrSample.map(SolrSample::getAccession);
  }
}
