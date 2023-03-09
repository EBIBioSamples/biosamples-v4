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
package uk.ac.ebi.biosamples.solr.service;

import java.util.*;
import java.util.AbstractMap.SimpleEntry;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.solr.core.query.Criteria;
import org.springframework.data.solr.core.query.FacetQuery;
import org.springframework.data.solr.core.query.FilterQuery;
import org.springframework.data.solr.core.query.SimpleFacetQuery;
import org.springframework.data.solr.core.query.result.FacetFieldEntry;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.model.facet.Facet;
import uk.ac.ebi.biosamples.model.facet.FacetHelper;
import uk.ac.ebi.biosamples.model.filter.Filter;
import uk.ac.ebi.biosamples.solr.model.field.SolrSampleField;
import uk.ac.ebi.biosamples.solr.repo.SolrSampleRepository;

@Service
public class SolrFacetService {
  private static final int TIME_ALLOWED = 55;
  private final SolrSampleRepository solrSampleRepository;
  private final SolrFieldService solrFieldService;
  private final SolrFilterService solrFilterService;

  public SolrFacetService(
      SolrSampleRepository solrSampleRepository,
      SolrFieldService solrFieldService,
      SolrFilterService solrFilterService) {
    this.solrSampleRepository = solrSampleRepository;
    this.solrFieldService = solrFieldService;
    this.solrFilterService = solrFilterService;
  }

  public List<Facet> getFacets(
      String searchTerm,
      Collection<Filter> filters,
      Collection<String> domains,
      Pageable facetFieldPageInfo,
      Pageable facetValuesPageInfo,
      String facetField) {
    boolean isLandingPage = false;

    List<Facet> facets = new ArrayList<>();
    FacetQuery query;
    if (StringUtils.isBlank(searchTerm) || "*:*".equals(searchTerm.trim())) {
      query = new SimpleFacetQuery(new Criteria().expression("*:*")); // default to search all
      isLandingPage = filters.isEmpty();
    } else {
      String lowerCasedSearchTerm = searchTerm.toLowerCase();
      // search for copied fields keywords_ss
//      query = new SimpleFacetQuery(new Criteria().expression("keywords_ss:" + lowerCasedSearchTerm));
      query = new SimpleFacetQuery(new Criteria("keywords_ss").fuzzy(lowerCasedSearchTerm));

      // boosting accession to bring accession matches to the top
      Criteria boostId = new Criteria("id").is(searchTerm).boost(5);
      boostId.setPartIsOr(true);
      query.addCriteria(boostId);

      // boosting name to bring name matches to the top
      Criteria boostName = new Criteria("name_s").is(searchTerm).boost(3);
      boostName.setPartIsOr(true);
      query.addCriteria(boostName);
    }
    query.setTimeAllowed(TIME_ALLOWED * 1000); // some facet queries could take longer to return

    // Add domains and release date filters
    Optional<FilterQuery> domainAndPublicFilterQuery =
        solrFilterService.getPublicFilterQuery(domains, null);
    domainAndPublicFilterQuery.ifPresent(query::addFilterQuery);

    // Add all the provided filters
    Optional<FilterQuery> optionalFilter = solrFilterService.getFilterQuery(filters);
    optionalFilter.ifPresent(query::addFilterQuery);

    List<Entry<SolrSampleField, Long>> allFacetFields =
        getFacetFields(facetFieldPageInfo, query, isLandingPage, facetField);

    List<Entry<SolrSampleField, Long>> rangeFacetFields = Collections.emptyList();
    if (facetField == null) {
      rangeFacetFields =
          FacetHelper.RANGE_FACETING_FIELDS.stream()
              .map(
                  s ->
                      new SimpleEntry<>(
                          this.solrFieldService.decodeField(s + FacetHelper.get_encoding_suffix(s)),
                          0L))
              .collect(Collectors.toList());
    }

    if (!allFacetFields.isEmpty()) {
      allFacetFields
          .get(0)
          .getKey()
          .getFacetCollectionStrategy()
          .fetchFacetsUsing(
              solrSampleRepository, query, allFacetFields, rangeFacetFields, facetValuesPageInfo)
          .forEach(opt -> opt.ifPresent(facets::add));
    }

    // Return the list of facets
    Collections.sort(facets);
    Collections.reverse(facets);

    return facets;
  }

  public List<Facet> getFacets(
      String searchTerm,
      Collection<Filter> filters,
      Collection<String> domains,
      Pageable facetFieldPageInfo,
      Pageable facetValuesPageInfo) {

    return getFacets(searchTerm, filters, domains, facetFieldPageInfo, facetValuesPageInfo, null);
  }

  private List<Entry<SolrSampleField, Long>> getFacetFields(
      Pageable facetFieldPageInfo, FacetQuery query, boolean isLandingPage, String facetField) {
    int facetLimit = 10;
    List<Entry<SolrSampleField, Long>> allFacetFields;

    // short-circuit for landing search page
    if (facetField != null) {
      allFacetFields =
          Collections.singletonList(
              new SimpleEntry<>(
                  solrFieldService.decodeField(
                      SolrFieldService.encodeFieldName(facetField)
                          + FacetHelper.get_encoding_suffix(facetField)),
                  0L));
    } else if (isLandingPage) {
      allFacetFields =
          FacetHelper.FACETING_FIELDS.stream()
              .limit(facetLimit)
              .map(
                  s ->
                      new SimpleEntry<>(
                          this.solrFieldService.decodeField(
                              SolrFieldService.encodeFieldName(s)
                                  + FacetHelper.get_encoding_suffix(s)),
                          0L))
              .collect(Collectors.toList());
    } else {
      allFacetFields = getDynamicFacetFields(facetFieldPageInfo, query, facetLimit);
    }

    return allFacetFields;
  }

  private List<Entry<SolrSampleField, Long>> getDynamicFacetFields(
      Pageable facetFieldPageInfo, FacetQuery query, int facetLimit) {
    List<Entry<SolrSampleField, Long>> allFacetFields = new ArrayList<>();
    Page<FacetFieldEntry> facetFields =
        solrSampleRepository.getFacetFields(query, facetFieldPageInfo);
    int facetCount = 0;
    for (FacetFieldEntry ffe : facetFields) {
      SolrSampleField solrSampleField = this.solrFieldService.decodeField(ffe.getValue());
      if (FacetHelper.FACETING_FIELDS.contains(solrSampleField.getReadableLabel())) {
        Long facetFieldCount = ffe.getValueCount();
        allFacetFields.add(new SimpleEntry<>(solrSampleField, facetFieldCount));
        if (++facetCount >= facetLimit) {
          break;
        }
      }
    }

    for (FacetFieldEntry ffe : facetFields) {
      if (facetCount++ >= facetLimit) {
        break;
      }
      SolrSampleField solrSampleField = this.solrFieldService.decodeField(ffe.getValue());
      if (!FacetHelper.FACETING_FIELDS.contains(solrSampleField.getReadableLabel())
          && !FacetHelper.IGNORE_FACETING_FIELDS.contains(solrSampleField.getReadableLabel())) {
        Long facetFieldCount = ffe.getValueCount();
        allFacetFields.add(new SimpleEntry<>(solrSampleField, facetFieldCount));
      }
    }

    return allFacetFields;
  }
}
