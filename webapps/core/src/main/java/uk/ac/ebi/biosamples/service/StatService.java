package uk.ac.ebi.biosamples.service;

import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.model.facet.Facet;
import uk.ac.ebi.biosamples.model.filter.Filter;
import uk.ac.ebi.biosamples.mongo.model.MongoAnalytics;
import uk.ac.ebi.biosamples.mongo.repo.MongoAnalyticsRepository;
import uk.ac.ebi.biosamples.solr.service.SolrFacetService;
import uk.ac.ebi.biosamples.solr.service.SolrFieldService;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class StatService {

  private final FacetService facetService;
  private final FilterService filterService;
  private final AnalyticsService analyticsService;
  private final SolrFacetService solrFacetService;
  private final SolrFieldService solrFieldService;

  public StatService(FacetService facetService, FilterService filterService,
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

  //this is not ideal, but we have hardcoded the data growth in this function
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
