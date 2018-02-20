package uk.ac.ebi.biosamples.solr.repo;

import java.io.IOException;
import java.util.List;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.CursorMarkParams;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.solr.core.QueryParsers;
import org.springframework.data.solr.core.SolrCallback;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.data.solr.core.convert.SolrConverter;
import org.springframework.data.solr.core.query.FacetOptions;
import org.springframework.data.solr.core.query.FacetQuery;
import org.springframework.data.solr.core.query.Query;
import org.springframework.data.solr.core.query.result.FacetFieldEntry;
import org.springframework.data.solr.core.query.result.FacetPage;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import uk.ac.ebi.biosamples.solr.model.SolrSample;

@Component
public class SolrSampleRepositoryImpl implements SolrSampleRepositoryCustom  {

	//this must be SolrTemplate not SolrOperations because we use some of the details
	private SolrTemplate solrTemplate;

	private final QueryParsers queryParsers = new QueryParsers();
	
	/**
	 * Constructor with required fields to build its own SolrOperations object
	 * because one is not normally exposed as a bean.
	 * 
	 * @param solrClient
	 * @param converter
	 */
	public SolrSampleRepositoryImpl(SolrClient solrClient, SolrConverter converter) {
		this.solrTemplate = createTemplate(solrClient, converter);
	}

	/**
	 * Private method to create a SolrTemplate object. Copied from SolrRepositoryFactory
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
	//TODO cacheing
	public Page<FacetFieldEntry> getFacetFields(FacetQuery query, Pageable facetPageable) {
		//configure the facet options to use the attribute types fields
		//and to have the appropriate paging
		FacetOptions facetOptions = new FacetOptions();
		facetOptions.addFacetOnField("facetfields_ss");
		facetOptions.setPageable(facetPageable);

		query.setFacetOptions(facetOptions);
		//execute the query against the solr server
		FacetPage<SolrSample> page = solrTemplate.queryForFacetPage(query, SolrSample.class);
		return page.getFacetResultPage("facetfields_ss");
	}

	@Override
	public FacetPage<?> getFacets(FacetQuery query, List<String> facetFields, Pageable facetPageable) {
		
		if (facetFields == null || facetFields.size() == 0) {
			throw new IllegalArgumentException("Must provide fields to facet on");
		}
		
		//configure the facet options to use the provided fields
		//and to have the appropriate paging
		FacetOptions facetOptions = new FacetOptions();
		for (String field : facetFields) {
			facetOptions.addFacetOnField(field);
		}
		facetOptions.setPageable(facetPageable);
	

		query.setFacetOptions(facetOptions);
		//execute the query against the solr server
		FacetPage<SolrSample> page = solrTemplate.queryForFacetPage(query, SolrSample.class);
		return page;
		
	}

	@Override
	public FacetPage<?> getRangeFacets(FacetQuery query, List<String> facetFields, Pageable facetPageable) {
	    //TODO Implement the method
		return null;
	}

	@Override
	public Page<SolrSample> findByQuery(Query query) {
		return solrTemplate.query(query, SolrSample.class);
	}

	@Override
	public CursorArrayList<SolrSample> findByQueryCursorMark(Query query, String cursorMark, int size) {
		
		//TODO this is a different set of query parsers than the solrOperation has itself
		SolrQuery solrQuery = queryParsers.getForClass(query.getClass()).constructSolrQuery(query);

		solrQuery.set(CursorMarkParams.CURSOR_MARK_PARAM, cursorMark);
		solrQuery.set(CommonParams.ROWS, size);
		
		QueryResponse response = solrTemplate.execute(new SolrCallback<QueryResponse>() {
			@Override
			public QueryResponse doInSolr(SolrClient solrClient) throws SolrServerException, IOException {
				return solrClient.query("samples", solrQuery);
			}
		});
		response.getNextCursorMark();
		List<SolrSample> solrSampleList = solrTemplate.convertQueryResponseToBeans(response, SolrSample.class);
		CursorArrayList<SolrSample> solrSampleCursorList = new CursorArrayList<SolrSample>(solrSampleList, response.getNextCursorMark());
		
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
