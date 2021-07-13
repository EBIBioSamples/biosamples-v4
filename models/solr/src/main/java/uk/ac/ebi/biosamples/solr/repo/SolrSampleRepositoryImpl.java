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

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.CursorMarkParams;
import org.apache.solr.common.params.FacetParams;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.solr.core.QueryParsers;
import org.springframework.data.solr.core.SolrCallback;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.data.solr.core.convert.SolrConverter;
import org.springframework.data.solr.core.query.*;
import org.springframework.data.solr.core.query.result.FacetFieldEntry;
import org.springframework.data.solr.core.query.result.FacetPage;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import uk.ac.ebi.biosamples.solr.model.SolrSample;

@Component
public class SolrSampleRepositoryImpl implements SolrSampleRepositoryCustom {

  // this must be SolrTemplate not SolrOperations because we use some of the details
  private SolrTemplate solrTemplate;

  private final QueryParsers queryParsers = new QueryParsers();

  /**
   * Constructor with required fields to build its own SolrOperations object because one is not
   * normally exposed as a bean.
   *
   * @param solrClient
   * @param converter
   */
  public SolrSampleRepositoryImpl(SolrClient solrClient, SolrConverter converter) {
    this.solrTemplate = createTemplate(solrClient, converter);
  }

  /**
   * Private method to create a SolrTemplate object. Copied from SolrRepositoryFactory
   *
   * @param solrClient
   * @param converter
   * @return
   */
  private SolrTemplate createTemplate(SolrClient solrClient, SolrConverter converter) {
    SolrTemplate template = new SolrTemplate(solrClient);
    if (converter != null) {
      template.setSolrConverter(converter);
    }
    template.afterPropertiesSet();
    return template;
  }

  @Override
  // TODO cacheing
  public Page<FacetFieldEntry> getFacetFields(FacetQuery query, Pageable facetPageable) {
    // configure the facet options to use the attribute types fields
    // and to have the appropriate paging
    FacetOptions facetOptions = new FacetOptions();
    facetOptions.addFacetOnField("facetfields_ss");
    facetOptions.setPageable(facetPageable);

    query.setFacetOptions(facetOptions);
    // execute the query against the solr server
    FacetPage<SolrSample> page = solrTemplate.queryForFacetPage(query, SolrSample.class);
    return page.getFacetResultPage("facetfields_ss");
  }

  @Override
  public FacetPage<?> getFacets(
      FacetQuery query, List<String> facetFields, Pageable facetPageable) {

    if (facetFields == null || facetFields.size() == 0) {
      throw new IllegalArgumentException("Must provide fields to facet on");
    }

    // configure the facet options to use the provided fields
    // and to have the appropriate paging
    FacetOptions facetOptions = new FacetOptions();
    for (String field : facetFields) {
      facetOptions.addFacetOnField(field);
    }
    facetOptions.setPageable(facetPageable);

    query.setFacetOptions(facetOptions);
    // execute the query against the solr server
    FacetPage<SolrSample> page = solrTemplate.queryForFacetPage(query, SolrSample.class);
    return page;
  }

  @Override
  public FacetPage<?> getFacets(
      FacetQuery query,
      List<String> facetFields,
      List<String> rangeFacetFields,
      Pageable facetPageable) {

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
    FacetOptions facetOptions = new FacetOptions();
    for (String field : facetFields) {
      facetOptions.addFacetOnField(field);
      facetOptions.addFacetQuery(new SimpleFacetQuery(new Criteria(field)));
    }
    facetOptions.setPageable(facetPageable);

    // todo generalise range facets apart from dates and remove hardcoded date boundaries
    for (String field : rangeFacetFields) {
      LocalDateTime dateTime = LocalDateTime.now();
      Date end = Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant());
      Date start = Date.from(dateTime.minusYears(5).atZone(ZoneId.systemDefault()).toInstant());
      facetOptions.addFacetByRange(
          new FacetOptions.FieldWithDateRangeParameters(field, start, end, "+1YEAR")
              .setInclude(FacetParams.FacetRangeInclude.ALL)
              .setOther(FacetParams.FacetRangeOther.BEFORE));
      facetOptions.addFacetQuery(new SimpleFacetQuery(new Criteria(field)));
    }

    query.setFacetOptions(facetOptions);
    // execute the query against the solr server
    FacetPage<SolrSample> page = solrTemplate.queryForFacetPage(query, SolrSample.class);
    return page;
  }

  @Override
  public FacetPage<?> getRangeFacets(
      FacetQuery query, List<String> facetFields, Pageable facetPageable) {
    // TODO Implement the method
    return null;
  }

  @Override
  public Page<SolrSample> findByQuery(Query query) {
    return solrTemplate.query(query, SolrSample.class);
  }

  @Override
  public CursorArrayList<SolrSample> findByQueryCursorMark(
      Query query, String cursorMark, int size) {

    // TODO this is a different set of query parsers than the solrOperation has itself
    SolrQuery solrQuery = queryParsers.getForClass(query.getClass()).constructSolrQuery(query);

    solrQuery.set(CursorMarkParams.CURSOR_MARK_PARAM, cursorMark);
    solrQuery.set(CommonParams.ROWS, size);

    QueryResponse response =
        solrTemplate.execute(
            new SolrCallback<QueryResponse>() {
              @Override
              public QueryResponse doInSolr(SolrClient solrClient)
                  throws SolrServerException, IOException {
                return solrClient.query("samples", solrQuery);
              }
            });
    response.getNextCursorMark();
    List<SolrSample> solrSampleList =
        solrTemplate.convertQueryResponseToBeans(response, SolrSample.class);
    CursorArrayList<SolrSample> solrSampleCursorList =
        new CursorArrayList<SolrSample>(solrSampleList, response.getNextCursorMark());

    return solrSampleCursorList;
  }

  @Override
  public FacetPage<SolrSample> findByFacetQuery(FacetQuery query) {
    return solrTemplate.queryForFacetPage(query, SolrSample.class);
  }

  @Override
  public SolrSample saveWithoutCommit(SolrSample entity) {
    Assert.notNull(entity, "Cannot save 'null' entity.");
    this.solrTemplate.saveBean(entity);
    return entity;
  }
}
