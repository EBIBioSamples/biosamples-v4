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

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.solr.core.query.*;
import org.springframework.data.solr.core.query.result.FacetFieldEntry;
import org.springframework.data.solr.core.query.result.FacetPage;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.model.Autocomplete;
import uk.ac.ebi.biosamples.model.filter.Filter;
import uk.ac.ebi.biosamples.solr.model.SolrSample;
import uk.ac.ebi.biosamples.solr.repo.CursorArrayList;
import uk.ac.ebi.biosamples.solr.repo.SolrSampleRepository;

@Service
public class SolrSampleService {
  private final SolrSampleRepository solrSampleRepository;
  private final SolrFilterService solrFilterService;

  // maximum time allowed for a solr search in s
  // TODO application.properties this
  private static final int TIMEALLOWED = 30;

  private Logger log = LoggerFactory.getLogger(getClass());

  public SolrSampleService(
      SolrSampleRepository solrSampleRepository, SolrFilterService solrFilterService) {
    this.solrSampleRepository = solrSampleRepository;
    this.solrFilterService = solrFilterService;
  }

  /**
   * Fetch the solr samples based on query specification
   *
   * @param searchTerm the term to search for in solr
   * @param filters a Collection of filters used in the solr query
   * @param domains a Collection of domains used in the solr query
   * @param pageable pagination information
   * @return a page of Samples full-filling the query
   */
  public Page<SolrSample> fetchSolrSampleByText(
      String searchTerm,
      Collection<Filter> filters,
      Collection<String> domains,
      String webinSubmissionAccountId,
      Pageable pageable) {
    Page<SolrSample> result;
    try {
      Query query = buildQuery(searchTerm, filters, domains, webinSubmissionAccountId);
      query.setPageRequest(pageable);
      query.setTimeAllowed(TIMEALLOWED * 1000);
      // return the samples from solr that match the query
      result = solrSampleRepository.findByQuery(query);
    } catch (Exception e) {
      // If it is not possible to use the search as a filter treat search string as text
      String escapedSearchTerm =
          searchTerm == null ? null : ClientUtils.escapeQueryChars(searchTerm);
      Query query = buildQuery(escapedSearchTerm, filters, domains, webinSubmissionAccountId);
      query.setPageRequest(pageable);
      query.setTimeAllowed(TIMEALLOWED * 1000);
      // return the samples from solr that match the query
      result = solrSampleRepository.findByQuery(query);
    }
    return result;
  }

  /**
   * Fetch the solr samples based on query specification
   *
   * @param searchTerm the term to search for in solr
   * @param filters a Collection of filters used in the solr query
   * @param domains a Collection of domains used in the solr query
   * @param cursorMark cursor serialization
   * @return a page of Samples full-filling the query
   */
  public CursorArrayList<SolrSample> fetchSolrSampleByText(
      String searchTerm,
      Collection<Filter> filters,
      Collection<String> domains,
      String webinSubmissionAccountId,
      String cursorMark,
      int size) {
    Query query = buildQuery(searchTerm, filters, domains, webinSubmissionAccountId);
    query.addSort(Sort.by("id")); // this must match the field in solr

    return solrSampleRepository.findByQueryCursorMark(query, cursorMark, size);
  }

  private Query buildQuery(
      String searchTerm,
      Collection<Filter> filters,
      Collection<String> domains,
      String webinSubmissionAccountId) {
    Query query;
    if (StringUtils.isBlank(searchTerm) || "*:*".equals(searchTerm.trim())) {
      query = new SimpleQuery("*:*"); // default to search all
    } else {
      String lowerCasedSearchTerm = searchTerm.toLowerCase();
      // search for copied fields keywords_ss and autocomplete_ss.
      // (think about merging them as autocomplete feature is not used)
      query = new SimpleQuery("keywords_ss:" + lowerCasedSearchTerm);
//      Criteria allAttributes = new SimpleStringCriteria("autocomplete_ss:" + searchTerm);
//      allAttributes.setPartIsOr(true);
//      query.addCriteria(allAttributes);

      // boosting accession to bring accession matches to the top
      Criteria boostId = new Criteria("id").is(searchTerm).boost(5);
      boostId.setPartIsOr(true);
      query.addCriteria(boostId);

      // boosting name to bring accession matches to the top
      Criteria boostName = new Criteria("name_s").is(searchTerm).boost(3);
      boostName.setPartIsOr(true);
      query.addCriteria(boostName);
    }

//    keywords_ss:Eukaryota
//    {!qf=keywords_ss}Eukaryota

//    SimpleQuery query = new SimpleQuery();
//    query.addCriteria(new Criteria("keywords_ss").is(searchTerm));
//    query.addCriteria(new SimpleStringCriteria(searchTerm));
//    query.setGroupOptions(new GroupOptions().addGroupByField("keywords_ss"));

    // build a query out of the users string and any facets
//    Query query = new SimpleQuery(queryField);

    query.addProjectionOnField(new SimpleField("id"));

    Optional<FilterQuery> publicFilterQuery =
        solrFilterService.getPublicFilterQuery(domains, webinSubmissionAccountId);
    publicFilterQuery.ifPresent(query::addFilterQuery);

    Optional<FilterQuery> optionalFilter = solrFilterService.getFilterQuery(filters);
    optionalFilter.ifPresent(query::addFilterQuery);

    return query;
  }

  public Autocomplete getAutocomplete(
      String autocompletePrefix, Collection<Filter> filters, int maxSuggestions) {

    // default to search all
    String searchTerm = "*:*";
    // build a query out of the users string and any facets
    FacetQuery query = new SimpleFacetQuery();
    query.addCriteria(new Criteria().expression(searchTerm));
    query.addProjectionOnField(new SimpleField("id"));

    Optional<FilterQuery> optionalFilter = solrFilterService.getFilterQuery(filters);
    optionalFilter.ifPresent(query::addFilterQuery);

    // filter out non-public
    Optional<FilterQuery> publicSampleFilterQuery =
        solrFilterService.getPublicFilterQuery(Collections.EMPTY_LIST, null);
    publicSampleFilterQuery.ifPresent(query::addFilterQuery);

    query.setPageRequest(PageRequest.of(0, 1));

    FacetOptions facetOptions = new FacetOptions();
    facetOptions.addFacetOnField("autocomplete_ss");
    facetOptions.setPageable(PageRequest.of(0, maxSuggestions));
    facetOptions.setFacetPrefix(autocompletePrefix);
    query.setFacetOptions(facetOptions);
    query.setTimeAllowed(TIMEALLOWED * 1000);

    FacetPage<?> facetPage = solrSampleRepository.findByFacetQuery(query);

    Page<FacetFieldEntry> facetFiledEntryPage = facetPage.getFacetResultPage("autocomplete_ss");

    List<String> autocompleted = new ArrayList<>();
    for (FacetFieldEntry facetFieldEntry : facetFiledEntryPage) {
      autocompleted.add(facetFieldEntry.getValue());
    }
    return new Autocomplete(autocompletePrefix, autocompleted);
  }
}
