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

import java.time.LocalDate;
import java.util.*;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.core.model.facet.Facet;
import uk.ac.ebi.biosamples.core.model.filter.Filter;
import uk.ac.ebi.biosamples.mongo.model.MongoAnalytics;
import uk.ac.ebi.biosamples.mongo.service.AnalyticsService;
import uk.ac.ebi.biosamples.solr.service.SolrFacetService;
import uk.ac.ebi.biosamples.solr.service.SolrFieldService;

@Service
public class StatService {

  private final FacetService facetService;
  private final FilterService filterService;
  private final AnalyticsService analyticsService;
  private final SolrFacetService solrFacetService;
  private final SolrFieldService solrFieldService;

  public StatService(
      final FacetService facetService,
      final FilterService filterService,
      final AnalyticsService analyticsService,
      final SolrFacetService solrFacetService,
      final SolrFieldService solrFieldService) {
    this.facetService = facetService;
    this.filterService = filterService;
    this.analyticsService = analyticsService;
    this.solrFacetService = solrFacetService;
    this.solrFieldService = solrFieldService;
  }

  public MongoAnalytics getStats() {
    final MongoAnalytics mongoAnalytics = analyticsService.getLatestAnalytics();
    return mongoAnalytics;
  }

  public Map<String, Integer> getBasicStat() {
    final Map<String, Integer> statMap = new HashMap<>();
    statMap.put("totalSampleCount", 0);
    statMap.put("totalCurationCount", 0);
    statMap.put("totalLinks", 0);

    return statMap;
  }

  // organims, tissue, sex
  public List<Facet> getSingleFacet(final String[] filter) {
    final Collection<Filter> filters = filterService.getFiltersCollection(filter);
    final Collection<String> domains = Collections.emptyList();
    final List<Facet> sampleFacets = facetService.getFacets("", filters, 1, 10, null);

    return sampleFacets;
  }

  public List<MongoAnalytics> getPipelineStats() {
    final LocalDate to = LocalDate.now();
    final LocalDate from = to.minusMonths(8);

    return analyticsService.getAnalytics(from, to);
  }

  // this is not ideal, but we have hardcoded the data growth in this function
  public Map<String, Integer> getBioSamplesYearlyGrowth() {
    final Map<String, Integer> growthMap = new HashMap<>();
    growthMap.put("2015", 3);
    growthMap.put("2016", 4);
    growthMap.put("2017", 7);
    growthMap.put("2018", 9);
    growthMap.put("2019", 12);
    growthMap.put("2020", 15);
    growthMap.put("2021", 20);
    return growthMap;
  }
}
