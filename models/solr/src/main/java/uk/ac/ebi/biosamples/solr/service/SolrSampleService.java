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
import org.springframework.data.solr.core.query.Criteria;
import org.springframework.data.solr.core.query.FacetOptions;
import org.springframework.data.solr.core.query.FacetQuery;
import org.springframework.data.solr.core.query.Field;
import org.springframework.data.solr.core.query.FilterQuery;
import org.springframework.data.solr.core.query.Query;
import org.springframework.data.solr.core.query.SimpleFacetQuery;
import org.springframework.data.solr.core.query.SimpleFilterQuery;
import org.springframework.data.solr.core.query.SimpleQuery;
import org.springframework.data.solr.core.query.SimpleStringCriteria;
import org.springframework.data.solr.core.query.result.FacetFieldEntry;
import org.springframework.data.solr.core.query.result.FacetPage;
import org.springframework.data.solr.core.query.result.FacetQueryResult;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;

import uk.ac.ebi.biosamples.solr.model.SampleFacets;
import uk.ac.ebi.biosamples.solr.model.SolrSample;
import uk.ac.ebi.biosamples.solr.repo.SolrSampleRepository;

@Service
public class SolrSampleService {

	private final SolrSampleRepository solrSampleRepository;	

	private Logger log = LoggerFactory.getLogger(getClass());
	
	public SolrSampleService(SolrSampleRepository solrSampleRepository) {
		this.solrSampleRepository = solrSampleRepository;
	}		
	
	//TODO add caching
	public Page<SolrSample> fetchSolrSampleByText(String searchTerm, MultiValueMap<String,String> filters, Pageable pageable) {
		//default to search all
		if (searchTerm == null || searchTerm.trim().length() == 0) {
			searchTerm = "*:*";
		}
		//build a query out of the users string and any facets
		Query query = new SimpleQuery(searchTerm);
				
		if (filters != null) {
			query = addFilters(query, filters);
		}		
		
		// return the samples from solr that match the query
		return solrSampleRepository.findByQuery(query, pageable);
	}	
	
	private <T extends Query> T addFilters(T query, MultiValueMap<String,String> filters) {
		//if no filters or filters are null, quick exit
		if (filters == null || filters.size() == 0) {
			return query;
		}		

		boolean filter = false;
		FilterQuery filterQuery = new SimpleFilterQuery();
		for (String facetType : filters.keySet()) {
			Criteria facetCriteria = null;
			
			//TODO facet name to/from facet field better
			String facetField = facetType+"_av_ss";
			for(String facatValue : filters.get(facetType)) {
				if (facatValue == null) {
					//no specific value, check if its not null
					facetCriteria = new Criteria(facetField).isNotNull();					
				} else if (facetCriteria == null) {
					facetCriteria = new Criteria(facetField).is(facatValue);
				} else {
					facetCriteria = facetCriteria.or(new Criteria(facetField).is(facatValue));
				}

				log.info("Filtering on "+facetField+" for value "+facatValue);
			}
			if (facetCriteria != null) {
				filterQuery.addCriteria(facetCriteria);
				filter = true;
			}
		}	
		if (filter) {
			query.addFilterQuery(filterQuery);
		}
		return query;
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
		
	public SampleFacets getFacets(String searchTerm, MultiValueMap<String,String> filters, Pageable facetPageable, Pageable facetValuePageable) {
		//default to search all
		if (searchTerm == null || searchTerm.trim().length() == 0) {
			searchTerm = "*:*";
		}
		
		//create a map to hold to temporarily total samples in each facet type
		Map<String, Long> rawFacetTotals = new HashMap<>();		

		//build a query out of the users string and any facets
		FacetQuery query = new SimpleFacetQuery();
		query.addCriteria(new Criteria().expression(searchTerm));
				
		if (filters != null && filters.size() > 0) {
			query = addFilters(query, filters);
		}		
		Page<FacetFieldEntry> facetFields = solrSampleRepository.getFacetFields(query, facetPageable);
		
		List<String> facetFieldList = new ArrayList<>();
		for (FacetFieldEntry ffe : facetFields) {
			//don't try to facet on things that are used little
			if (ffe.getValueCount() > 0) {
				rawFacetTotals.put(ffe.getValue(), ffe.getValueCount());
				facetFieldList.add(ffe.getValue());
				log.info("Putting "+ffe.getValue()+" with count "+ffe.getValueCount());
			}
		}
		
		//convert the temporary map to a more permanent sorted map
		MapComparableComparator<String, Long> facetTotalComparator = new MapComparableComparator<>(rawFacetTotals);
		SortedMap<String, Long> facetTotals = new TreeMap<>(facetTotalComparator);
		facetTotals.putAll(rawFacetTotals);
		
		SortedMap<String, SortedMap<String, Long>> facets = new TreeMap<>(facetTotalComparator);
		
		//if there are no facets avaliable (i.e. no samples)
		//then cleanly exit here
		if (facetTotals.isEmpty()) {
			return SampleFacets.build(facets, facetTotals);
		}

		//create a nested map to the facets themselves
		FacetPage<?> facetPage = solrSampleRepository.getFacets(query, facetFieldList, facetValuePageable);
		for (Field field : facetPage.getFacetFields()) {
			
			log.info("Checking field "+field.getName());
			
			//create a map to hold the values and counts for this facet
			Map<String, Long> rawFieldMap = new HashMap<>();	
			
			//for each value, put the number of them into this facets map
			for (FacetFieldEntry ffe : facetPage.getFacetResultPage(field)) {
				if (ffe.getValueCount() > 0) {
					log.info("Adding "+ffe.getValue()+" with count "+ffe.getValueCount()+" in "+field.getName());
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
