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

import org.apache.solr.client.solrj.util.ClientUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.core.model.facet.Facet;
import uk.ac.ebi.biosamples.core.model.filter.Filter;
import uk.ac.ebi.biosamples.service.facet.FacetService;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;

@Service
public class FacetingService {
  private final Logger log = LoggerFactory.getLogger(getClass());

  private final FacetService facetService;

  public FacetingService(@Qualifier("elasticFacetService") FacetService facetService) {
    this.facetService = facetService;
  }

  public List<Facet> getFacets(
      final String text,
      final Collection<Filter> filters,
      final int noOfFacets,
      final int noOfFacetValues,
      final List<String> facetFields) {
    return getFacets(text, filters, noOfFacets, noOfFacetValues, null, facetFields);
  }

  public List<Facet> getFacets(
      final String text,
      final Collection<Filter> filters,
      final int noOfFacets,
      final int noOfFacetValues,
      final String facetField,
      final List<String> facetFields) {
    final Pageable facetPageable = PageRequest.of(0, noOfFacets);
    final Pageable facetValuePageable = PageRequest.of(0, noOfFacetValues);
    // TODO if a facet is enabled as a filter, then that value will be the only filter displayed
    // TODO allow update date range

    // TODO if (text == null && filters.isEmpty()) cache results for the search landing page
    final long startTime = System.nanoTime();
    final String escapedText = text == null ? null : ClientUtils.escapeQueryChars(text);
    final List<Facet> facets =
        facetService.getFacets(
            escapedText, new HashSet<>(filters), null, facetPageable, facetValuePageable, facetField, facetFields);
    final long endTime = System.nanoTime();
    log.trace("Got solr facets in " + ((endTime - startTime) / 1000000) + "ms");

    return facets;
  }
}
