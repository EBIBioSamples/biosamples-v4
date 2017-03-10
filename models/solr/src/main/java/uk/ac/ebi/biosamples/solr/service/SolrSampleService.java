package uk.ac.ebi.biosamples.solr.service;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.solr.client.solrj.SolrClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.solr.core.SolrOperations;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.data.solr.core.convert.SolrConverter;
import org.springframework.data.solr.core.query.FacetOptions;
import org.springframework.data.solr.core.query.FacetQuery;
import org.springframework.data.solr.core.query.Field;
import org.springframework.data.solr.core.query.SimpleFacetQuery;
import org.springframework.data.solr.core.query.SimpleStringCriteria;
import org.springframework.data.solr.core.query.result.FacetFieldEntry;
import org.springframework.data.solr.core.query.result.FacetPage;
import org.springframework.data.solr.repository.support.SolrEntityInformationCreatorImpl;
import org.springframework.data.solr.server.SolrClientFactory;
import org.springframework.data.solr.server.support.MulticoreSolrClientFactory;
import org.springframework.data.solr.server.support.SolrClientUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.solr.model.SolrSample;
import uk.ac.ebi.biosamples.solr.repo.SolrSampleRepository;

@Service
public class SolrSampleService {

	private final SolrSampleRepository solrSampleRepository;	
	
	private final SolrOperations solrOperations;
	
	public SolrSampleService(SolrSampleRepository solrSampleRepository, @Qualifier(value="solrOperationsSample") SolrOperations solrOperations) {
		this.solrOperations = solrOperations;
		this.solrSampleRepository = solrSampleRepository;
	}	
	
	
	/**
	 * This will get both a page of results and the facets associated with that query. It will call solr multiple
	 * times to do this correctly. In future, it will implement caching where possible to minimise those calls.
	 * 
	 * @param text
	 * @param pageable
	 * @return
	 */
	//TODO add caching
	public FacetPage<SolrSample> fetchSolrSampleByText(String text, Pageable pageable) {
		// return the samples from solr that match the query
		
		
		//do one query to get the facets to use for the second query
		
		FacetPage<SolrSample> facetTypes = solrSampleRepository.findByTextAndPublicWithFacetTypes(text, pageable);
		List<FacetFieldEntry> attributeTypeFacet = facetTypes.getFacetResultPage("attributetypes_ss").getContent();
		
		if (attributeTypeFacet.size() == 0) {
			//no facets return, so just return this result
			return facetTypes;
		}
				
		//add the previously retrieved attribute types as facets for the second query
		FacetOptions facetOptions = new FacetOptions();
		facetTypes.getFacetResultPage("attributetypes_ss").getContent().stream()
			.map(ffe -> ffe.getField())
			.forEachOrdered(f -> facetOptions.addFacetOnField(f));
		//build the query using the existing text string, the page information, and the dynamic facets
		FacetQuery query = new SimpleFacetQuery(new SimpleStringCriteria(text), pageable).setFacetOptions(facetOptions);
		//execute the query against the solr server
		FacetPage<SolrSample> page = solrOperations.queryForFacetPage(query, SolrSample.class);
		return page;
	}

}
