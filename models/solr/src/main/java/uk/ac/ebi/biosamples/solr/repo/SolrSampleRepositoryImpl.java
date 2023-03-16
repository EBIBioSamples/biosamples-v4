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
package uk.ac.ebi.biosamples.solr.repo;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.CursorMarkParams;
import org.apache.solr.common.params.FacetParams;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.solr.core.QueryParsers;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.data.solr.core.mapping.SimpleSolrMappingContext;
import org.springframework.data.solr.core.query.*;
import org.springframework.data.solr.core.query.result.FacetFieldEntry;
import org.springframework.data.solr.core.query.result.FacetPage;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import uk.ac.ebi.biosamples.solr.model.SolrSample;

@Component
public class SolrSampleRepositoryImpl implements SolrSampleRepositoryCustom {
  // this must be SolrTemplate not SolrOperations because we use some of the details
  private final SolrTemplate solrTemplate;
  private final QueryParsers queryParsers = new QueryParsers(new SimpleSolrMappingContext());

  /**
   * Constructor with required fields to build its own SolrOperations object because one is not
   * normally exposed as a bean.
   *
   * @param solrClient
   */
  public SolrSampleRepositoryImpl(final SolrClient solrClient) {
    solrTemplate = createTemplate(solrClient);
  }

  /**
   * Private method to create a SolrTemplate object. Copied from SolrRepositoryFactory
   *
   * @param solrClient
   * @return
   */
  private SolrTemplate createTemplate(final SolrClient solrClient) {
    final SolrTemplate template = new SolrTemplate(solrClient);
    template.afterPropertiesSet();
    return template;
  }

  @Override
  // TODO cacheing
  public Page<FacetFieldEntry> getFacetFields(
      final FacetQuery query, final Pageable facetPageable) {
    // configure the facet options to use the attribute types fields
    // and to have the appropriate paging
    final FacetOptions facetOptions = new FacetOptions();
    facetOptions.addFacetOnField("facetfields_ss");
    facetOptions.setPageable(facetPageable);

    query.setFacetOptions(facetOptions);
    // execute the query against the solr server
    final FacetPage<SolrSample> page =
        solrTemplate.queryForFacetPage("samples", query, SolrSample.class);
    return page.getFacetResultPage("facetfields_ss");
  }

  @Override
  public FacetPage<?> getFacets(
      final FacetQuery query, final List<String> facetFields, final Pageable facetPageable) {

    if (facetFields == null || facetFields.size() == 0) {
      throw new IllegalArgumentException("Must provide fields to facet on");
    }

    // configure the facet options to use the provided fields
    // and to have the appropriate paging
    final FacetOptions facetOptions = new FacetOptions();
    for (final String field : facetFields) {
      facetOptions.addFacetOnField(field);
    }
    facetOptions.setPageable(facetPageable);

    query.setFacetOptions(facetOptions);
    // execute the query against the solr server
    return solrTemplate.queryForFacetPage("samples", query, SolrSample.class);
  }

  @Override
  public FacetPage<?> getFacets(
      final FacetQuery query,
      final List<String> facetFields,
      final List<String> rangeFacetFields,
      final Pageable facetPageable) {

    if (facetFields == null || facetFields.isEmpty()) {
      throw new IllegalArgumentException("Must provide fields to facet on");
    }

    //    //if we are retrieving large number of facets limit for front page, remove this for new
    // implementation
    //    if (query.getFilterQueries().size() <= 1 &&
    //            query.getCriteria().getPredicates().stream().filter(f ->
    // f.getValue().equals("*:*")).count() >= 1) {
    //      facetFields = facetFields.subList(0, Math.min(facetPageable.getPageSize(),
    // facetFields.size()));
    //    }

    // configure the facet options to use the provided fields
    // and to have the appropriate paging
    final FacetOptions facetOptions = new FacetOptions();
    for (final String field : facetFields) {
      facetOptions.addFacetOnField(field);
      facetOptions.addFacetQuery(new SimpleFacetQuery(new Criteria(field)));
    }
    facetOptions.setPageable(facetPageable);

    // todo generalise range facets apart from dates and remove hardcoded date boundaries
    for (final String field : rangeFacetFields) {
      final LocalDateTime dateTime = LocalDateTime.now();
      final Date end = Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant());
      final Date start =
          Date.from(dateTime.minusYears(5).atZone(ZoneId.systemDefault()).toInstant());
      facetOptions.addFacetByRange(
          new FacetOptions.FieldWithDateRangeParameters(field, start, end, "+1YEAR")
              .setInclude(FacetParams.FacetRangeInclude.ALL)
              .setOther(FacetParams.FacetRangeOther.BEFORE));
      facetOptions.addFacetQuery(new SimpleFacetQuery(new Criteria(field)));
    }

    query.setFacetOptions(facetOptions);
    // execute the query against the solr server
    return solrTemplate.queryForFacetPage("samples", query, SolrSample.class);
  }

  @Override
  public FacetPage<?> getRangeFacets(
      final FacetQuery query, final List<String> facetFields, final Pageable facetPageable) {
    // TODO Implement the method
    return null;
  }

  @Override
  public Page<SolrSample> findByQuery(final Query query) {
    return solrTemplate.query("samples", query, SolrSample.class);
  }

  @Override
  public CursorArrayList<SolrSample> findByQueryCursorMark(
      final Query query, final String cursorMark, final int size) {

    // TODO this is a different set of query parsers than the solrOperation has itself
    final SolrQuery solrQuery =
        queryParsers.getForClass(query.getClass()).constructSolrQuery(query, SolrSample.class);

    solrQuery.set(CursorMarkParams.CURSOR_MARK_PARAM, cursorMark);
    solrQuery.set(CommonParams.ROWS, size);

    final QueryResponse response =
        solrTemplate.execute(solrClient -> solrClient.query("samples", solrQuery));
    response.getNextCursorMark();
    final List<SolrSample> solrSampleList =
        solrTemplate.convertQueryResponseToBeans(response, SolrSample.class);

    return new CursorArrayList<>(solrSampleList, response.getNextCursorMark());
  }

  @Override
  public FacetPage<SolrSample> findByFacetQuery(final FacetQuery query) {
    return solrTemplate.queryForFacetPage("samples", query, SolrSample.class);
  }

  @Override
  public SolrSample saveWithoutCommit(final SolrSample entity) {
    Assert.notNull(entity, "Cannot save 'null' entity.");
    solrTemplate.saveBean("samples", entity);
    return entity;
  }
}
