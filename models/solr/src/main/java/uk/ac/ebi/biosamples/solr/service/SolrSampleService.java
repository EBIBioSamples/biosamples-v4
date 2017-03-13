package uk.ac.ebi.biosamples.solr.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.solr.core.SolrOperations;
import org.springframework.data.solr.core.query.FacetOptions;
import org.springframework.data.solr.core.query.FacetQuery;
import org.springframework.data.solr.core.query.Field;
import org.springframework.data.solr.core.query.SimpleFacetQuery;
import org.springframework.data.solr.core.query.SimpleStringCriteria;
import org.springframework.data.solr.core.query.result.FacetFieldEntry;
import org.springframework.data.solr.core.query.result.FacetPage;
import org.springframework.data.solr.core.query.result.FacetQueryResult;
import org.springframework.stereotype.Service;

import uk.ac.ebi.biosamples.solr.model.SampleFacets;
import uk.ac.ebi.biosamples.solr.model.SolrSample;
import uk.ac.ebi.biosamples.solr.repo.SolrSampleRepository;

@Service
public class SolrSampleService {

	private final SolrSampleRepository solrSampleRepository;	
	
	private final SolrOperations solrOperations;

	private Logger log = LoggerFactory.getLogger(getClass());
	
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
	public FacetPage<SolrSample> fetchSolrSampleByText(String text, Pageable pageable, Pageable facetPageable) {
				
		//do one query to get the facets to use for the second query
		Page<FacetFieldEntry> facetFields = solrSampleRepository.getFacetFields(text, facetPageable);
		
		//add the previously retrieved attribute types as facets for the second query
		FacetOptions facetOptions = new FacetOptions();
		for (FacetFieldEntry ffe : facetFields) {
			facetOptions.addFacetOnField(ffe.getValue());
		}
		
		if (facetOptions.getFacetOnFields().size() == 0) {
			// return the samples from solr that match the query
			return solrSampleRepository.findByTextAndPublic(text, pageable);
		} else {
			//build the query using the existing text string, the page information, and the dynamic facets
			FacetQuery query = new SimpleFacetQuery(new SimpleStringCriteria(text), pageable).setFacetOptions(facetOptions);
			//execute the query against the solr server
			FacetPage<SolrSample> page = solrOperations.queryForFacetPage(query, SolrSample.class);
			
			return page;
		}
	}	

	private static class MapComparableComparator<S, T extends Comparable<T>> implements Comparator<S> {
		
		private final Map<S, T> mapComparable;
		
		public MapComparableComparator(Map<S, T> mapComparable) {
			this.mapComparable = mapComparable;
		}
		
		@Override
		public int compare(S a, S b) {
			
			return mapComparable.get(a).compareTo(mapComparable.get(b));
		}		
	}
	
	
	
	public SampleFacets getFacets(String text, Pageable facetPageable, Pageable facetValuePageable) {
		
		//create a map to hold to temporarily total samples in each facet type
		Map<String, Long> rawFacetTotals = new HashMap<>();		

		Page<FacetFieldEntry> facetFields = solrSampleRepository.getFacetFields(text, facetPageable);
		List<String> facetFieldList = new ArrayList<>();
		for (FacetFieldEntry ffe : facetFields) {
			//don't try to facet on things that are used little
			if (ffe.getValueCount() > 0) {
				rawFacetTotals.put(ffe.getValue(), ffe.getValueCount());
				facetFieldList.add(ffe.getValue());
				log.trace("Putting "+ffe.getValue()+" with count "+ffe.getValueCount());
			}
		}
		
		//convert the temporary map to a more permanent sorted map
		MapComparableComparator<String, Long> facetTotalComparator = new MapComparableComparator<>(rawFacetTotals);
		SortedMap<String, Long> facetTotals = new TreeMap<>(facetTotalComparator);
		facetTotals.putAll(rawFacetTotals);
		

		//create a nested map to the facets themselves
		SortedMap<String, SortedMap<String, Long>> facets = new TreeMap<>(facetTotalComparator);
		FacetPage<?> facetPage = solrSampleRepository.getFacets(text, facetFieldList, facetValuePageable);
		for (Field field : facetPage.getFacetFields()) {
			
			log.trace("Checking field "+field.getName());
			
			//create a map to hold the values and counts for this facet
			Map<String, Long> rawFieldMap = new HashMap<>();	
			
			//for each value, put the number of them into this facets map
			for (FacetFieldEntry ffe : facetPage.getFacetResultPage(field)) {
				if (ffe.getValueCount() > 0) {
					log.trace("Adding "+ffe.getValue()+" with count "+ffe.getValueCount()+" in "+field.getName());
					rawFieldMap.put(ffe.getValue(), ffe.getValueCount());
				}
			}

			//convert the temporary map to a more permanent sorted map
			MapComparableComparator<String, Long> fieldComparator = new MapComparableComparator<>(rawFieldMap);
			SortedMap<String, Long> fieldMap = new TreeMap<>(fieldComparator);
			fieldMap.putAll(rawFieldMap);
			//add this facets map into the overall map
			//but only if its big enough to be useful
			if (fieldMap.size() > 0) {
				facets.put(field.getName(), fieldMap);
			}
		}
		
		return SampleFacets.build(facets, facetTotals);
	}
}
