/*
* Copyright 2019 EMBL - European Bioinformatics Institute
* Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
* file except in compliance with the License. You may obtain a copy of the License at
* http://www.apache.org/licenses/LICENSE-2.0
* Unless required by applicable law or agreed to in writing, software distributed under the
* License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
* CONDITIONS OF ANY KIND, either express or implied. See the License for the
* specific language governing permissions and limitations under the License.
*/
package uk.ac.ebi.biosamples.solr.repo;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.solr.core.query.FacetQuery;
import org.springframework.data.solr.core.query.Query;
import org.springframework.data.solr.core.query.result.FacetFieldEntry;
import org.springframework.data.solr.core.query.result.FacetPage;
import uk.ac.ebi.biosamples.solr.model.SolrSample;

public interface SolrSampleRepositoryCustom {

  /**
   * Get the attribute types (or other facet fields) for a particular query and using the pageable
   * to determine the number of offset of *the facets* returned as a page of facet fields
   *
   * @param test
   * @param facetPageable
   * @return
   */
  public Page<FacetFieldEntry> getFacetFields(FacetQuery query, Pageable facetPageable);

  /**
   * Return a result of facets over the provided fields with the provided facet paging information
   * (offset and count).
   *
   * @param query
   * @param facetFields
   * @param facetPageable
   * @return
   */
  public FacetPage<?> getFacets(FacetQuery query, List<String> facetFields, Pageable facetPageable);

  /**
   * Return a result of facets over the provided fields with the provided facet paging information
   * (offset and count).
   *
   * @param query
   * @param facetFields
   * @param rangeFacetFields
   * @param facetPageable
   * @return
   */

  public FacetPage<?> getFacets(FacetQuery query, List<String> facetFields, List<String> rangeFacetFields, Pageable facetPageable);

  /**
   * Return a results of range facets over the provided fields with the provided facet paging
   * information (offset and count).
   *
   * @param query
   * @param facetFields
   * @param facetPageable
   * @return
   */
  public FacetPage<?> getRangeFacets(
      FacetQuery query, List<String> facetFields, Pageable facetPageable);

  /**
   * Use a query object to get a page of results. This allows for more complicated query
   * construction compared to a simple string e.g. filtering
   *
   * @param query
   * @return
   */
  public Page<SolrSample> findByQuery(Query query);

  /**
   * Use a query object to get a page of results. This allows for more complicated query
   * construction compared to a simple string e.g. filtering
   *
   * @param query
   * @return
   */
  public FacetPage<SolrSample> findByFacetQuery(FacetQuery query);

  public SolrSample saveWithoutCommit(SolrSample entity);

  public CursorArrayList<SolrSample> findByQueryCursorMark(
      Query query, String cursorMark, int size);
}
