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

import java.util.Collection;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.solr.core.query.*;
import org.springframework.stereotype.Service;
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
  private static final int TIMEALLOWED = 55;

  public SolrSampleService(
      final SolrSampleRepository solrSampleRepository, final SolrFilterService solrFilterService) {
    this.solrSampleRepository = solrSampleRepository;
    this.solrFilterService = solrFilterService;
  }

  /**
   * Fetch the solr samples based on query specification
   *
   * @param searchTerm the term to search for in solr
   * @param filters a Collection of filters used in the solr query
   * @param pageable pagination information
   * @return a page of Samples full-filling the query
   */
  public Page<SolrSample> fetchSolrSampleByText(
      final String searchTerm,
      final Collection<Filter> filters,
      final String webinSubmissionAccountId,
      final Pageable pageable) {
    Page<SolrSample> result;
    try {
      final Query query = buildQuery(searchTerm, filters, webinSubmissionAccountId);
      query.setPageRequest(pageable);
      query.setTimeAllowed(TIMEALLOWED * 1000);
      // return the samples from solr that match the query
      result = solrSampleRepository.findByQuery(query);
    } catch (final Exception e) {
      // If it is not possible to use the search as a filter treat search string as text
      final String escapedSearchTerm =
          searchTerm == null ? null : ClientUtils.escapeQueryChars(searchTerm);
      final Query query = buildQuery(escapedSearchTerm, filters, webinSubmissionAccountId);
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
   * @param cursorMark cursor serialization
   * @return a page of Samples full-filling the query
   */
  public CursorArrayList<SolrSample> fetchSolrSampleByText(
      final String searchTerm,
      final Collection<Filter> filters,
      final String webinSubmissionAccountId,
      final String cursorMark,
      final int size) {
    final Query query = buildQuery(searchTerm, filters, webinSubmissionAccountId);
    query.addSort(Sort.by("id")); // this must match the field in solr

    return solrSampleRepository.findByQueryCursorMark(query, cursorMark, size);
  }

  private Query buildQuery(
      final String searchTerm,
      final Collection<Filter> filters,
      final String webinSubmissionAccountId) {
    final Query query;
    if (StringUtils.isBlank(searchTerm) || "*:*".equals(searchTerm.trim())) {
      query = new SimpleQuery("*:*"); // default to search all
    } else {
      final String lowerCasedSearchTerm = searchTerm.toLowerCase();

      // search for copied fields keywords_ss.
      query = new SimpleQuery();
      final Criteria searchCriteria = new Criteria("keywords_ss").fuzzy(lowerCasedSearchTerm);
      searchCriteria.setPartIsOr(true);
      query.addCriteria(searchCriteria);

      // boosting accession to bring accession matches to the top
      final Criteria boostId = new Criteria("id").is(searchTerm).boost(5);
      boostId.setPartIsOr(true);
      query.addCriteria(boostId);

      // boosting name to bring accession matches to the top
      final Criteria boostName = new Criteria("name_s").is(searchTerm).boost(3);
      boostName.setPartIsOr(true);
      query.addCriteria(boostName);
    }

    query.addProjectionOnField(new SimpleField("id"));

    final Optional<FilterQuery> publicFilterQuery =
        solrFilterService.getPublicFilterQuery(webinSubmissionAccountId);
    publicFilterQuery.ifPresent(query::addFilterQuery);

    solrFilterService.getFilterQuery(filters).forEach(query::addFilterQuery);

    return query;
  }
}
