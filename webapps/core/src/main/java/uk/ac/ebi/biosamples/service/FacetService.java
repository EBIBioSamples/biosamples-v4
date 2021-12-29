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
import java.util.List;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.model.facet.Facet;
import uk.ac.ebi.biosamples.model.filter.Filter;
import uk.ac.ebi.biosamples.solr.service.SolrFacetService;

@Service
public class FacetService {

  private Logger log = LoggerFactory.getLogger(getClass());

  private final SolrFacetService solrFacetService;

  public FacetService(SolrFacetService solrSampleService) {
    this.solrFacetService = solrSampleService;
  }

  public List<Facet> getFacets(
      String text,
      Collection<Filter> filters,
      Collection<String> domains,
      int noOfFacets,
      int noOfFacetValues) {
    return getFacets(text, filters, domains, noOfFacets, noOfFacetValues, null);
  }

  public List<Facet> getFacets(
      String text,
      Collection<Filter> filters,
      Collection<String> domains,
      int noOfFacets,
      int noOfFacetValues,
      String facetField) {
    Pageable facetPageable = new PageRequest(0, noOfFacets);
    Pageable facetValuePageable = new PageRequest(0, noOfFacetValues);
    // TODO if a facet is enabled as a filter, then that value will be the only filter displayed
    // TODO allow update date range

    // todo if (text == null && filters.isEmpty()) cache results for the search landing page
    long startTime = System.nanoTime();
    String escapedText = text == null ? null : ClientUtils.escapeQueryChars(text);
    List<Facet> facets =
        solrFacetService.getFacets(
            escapedText, filters, domains, facetPageable, facetValuePageable, facetField);
    long endTime = System.nanoTime();
    log.trace("Got solr facets in " + ((endTime - startTime) / 1000000) + "ms");

    return facets;
  }
}
