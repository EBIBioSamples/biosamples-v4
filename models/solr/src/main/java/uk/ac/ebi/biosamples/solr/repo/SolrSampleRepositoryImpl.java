package uk.ac.ebi.biosamples.solr.repo;

import java.util.List;

import org.apache.solr.client.solrj.SolrClient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.solr.core.SolrOperations;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.data.solr.core.convert.SolrConverter;
import org.springframework.data.solr.core.query.FacetOptions;
import org.springframework.data.solr.core.query.FacetQuery;
import org.springframework.data.solr.core.query.Query;
import org.springframework.data.solr.core.query.SimpleField;
import org.springframework.data.solr.core.query.result.FacetFieldEntry;
import org.springframework.data.solr.core.query.result.FacetPage;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import uk.ac.ebi.biosamples.solr.model.SolrSample;

@Component
public class SolrSampleRepositoryImpl implements SolrSampleRepositoryCustom  {

	private SolrOperations solrOperations;
	
	
	/**
	 * Constructor with required fields to build its own SolrOperations object
	 * becuase one is not normally exposed as a bean.
	 * 
	 * @param solrClient
	 * @param converter
	 */
	public SolrSampleRepositoryImpl(SolrClient solrClient, SolrConverter converter) {
		this.solrOperations = createTemplate(solrClient, converter);
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
		FacetPage<SolrSample> page = solrOperations.queryForFacetPage(query, SolrSample.class);
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
		FacetPage<SolrSample> page = solrOperations.queryForFacetPage(query, SolrSample.class);
		return page;
		
	}

	@Override
	public FacetPage<?> getRangeFacets(FacetQuery query, List<String> facetFields, Pageable facetPageable) {
	    //TODO Implement the method
		return null;
	}

	@Override
	public Page<SolrSample> findByQuery(Query query) {
		return solrOperations.query(query, SolrSample.class);
	}


	@Override
	public FacetPage<SolrSample> findByFacetQuery(FacetQuery query) {
		return solrOperations.queryForFacetPage(query, SolrSample.class);
	}

	@Override
	public SolrSample saveWithoutCommit(SolrSample entity) {
		Assert.notNull(entity, "Cannot save 'null' entity.");
		this.solrOperations.saveBean(entity);
		return entity;
	}
}
