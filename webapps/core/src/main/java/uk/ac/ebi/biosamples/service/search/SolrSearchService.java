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
package uk.ac.ebi.biosamples.service.search;

import io.micrometer.core.annotation.Timed;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.core.model.filter.Filter;
import uk.ac.ebi.biosamples.solr.model.SolrSample;
import uk.ac.ebi.biosamples.solr.repo.CursorArrayList;
import uk.ac.ebi.biosamples.solr.service.SolrSampleService;

@Service("solrSearchService")
@RequiredArgsConstructor
@Slf4j
public class SolrSearchService implements SearchService {
  private final SolrSampleService solrSampleService;

  @Override
  @Timed("biosamples.search.page.solr")
  public Page<String> searchForAccessions(
      String searchTerm, Set<Filter> filters, String webinId, Pageable pageable) {
    return solrSampleService
        .fetchSolrSampleByText(searchTerm, filters, webinId, pageable)
        .map(SolrSample::getAccession);
  }

  @Override
  @Timed("biosamples.search.cursor.solr")
  public CursorArrayList<String> searchForAccessions(
      String searchTerm, Set<Filter> filters, String webinId, String cursor, int size) {
    CursorArrayList<SolrSample> samples =
        solrSampleService.fetchSolrSampleByText(searchTerm, filters, webinId, cursor, size);
    List<String> accessions = samples.stream().map(SolrSample::getAccession).toList();
    return new CursorArrayList<>(accessions, samples.getNextCursorMark());
  }
}
