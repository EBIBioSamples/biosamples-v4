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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.model.facet.Facet;
import uk.ac.ebi.biosamples.model.filter.Filter;
import uk.ac.ebi.biosamples.mongo.model.MongoAnalytics;
import uk.ac.ebi.biosamples.solr.service.SolrFacetService;
import uk.ac.ebi.biosamples.solr.service.SolrFieldService;
import uk.ac.ebi.biosamples.utils.mongo.AnalyticsService;

@Service
public class StatService {

  private final FacetService facetService;
  private final FilterService filterService;
  private final AnalyticsService analyticsService;
  private final SolrFacetService solrFacetService;
  private final SolrFieldService solrFieldService;

  public StatService(
      FacetService facetService,
      FilterService filterService,
      AnalyticsService analyticsService,
      SolrFacetService solrFacetService,
      SolrFieldService solrFieldService) {
    this.facetService = facetService;
    this.filterService = filterService;
    this.analyticsService = analyticsService;
    this.solrFacetService = solrFacetService;
    this.solrFieldService = solrFieldService;
  }

  public MongoAnalytics getStats() {
    MongoAnalytics mongoAnalytics = analyticsService.getLatestAnalytics();
    return mongoAnalytics;
  }

  public Map<String, Integer> getBasicStat() {
    Map<String, Integer> statMap = new HashMap<>();
    statMap.put("totalSampleCount", 0);
    statMap.put("totalCurationCount", 0);
    statMap.put("totalLinks", 0);

    return statMap;
  }

  // organims, tissue, sex
  public List<Facet> getSingleFacet(String[] filter) {
    Collection<Filter> filters = filterService.getFiltersCollection(filter);
    Collection<String> domains = Collections.emptyList();
    List<Facet> sampleFacets = facetService.getFacets("", filters, domains, 1, 10);

    return sampleFacets;
  }

  public List<MongoAnalytics> getPipelineStats() {
    LocalDate to = LocalDate.now();
    LocalDate from = to.minusMonths(8);

    return analyticsService.getAnalytics(from, to);
  }

  // this is not ideal, but we have hardcoded the data growth in this function
  public Map<String, Integer> getBioSamplesYearlyGrowth() {
    Map<String, Integer> growthMap = new HashMap<>();
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
