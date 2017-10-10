package uk.ac.ebi.biosamples.solr.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.solr.core.query.*;
import org.springframework.data.solr.core.query.result.FacetFieldEntry;
import org.springframework.data.solr.core.query.result.FacetPage;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import uk.ac.ebi.biosamples.BioSamplesProperties;
import uk.ac.ebi.biosamples.model.Autocomplete;
import uk.ac.ebi.biosamples.model.filters.Filter;
import uk.ac.ebi.biosamples.solr.model.SolrSample;
import uk.ac.ebi.biosamples.solr.repo.SolrSampleRepository;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Service
public class SolrSampleService {

	private final SolrSampleRepository solrSampleRepository;

	private final BioSamplesProperties bioSamplesProperties;

	private final SolrFacetService solrFacetService;
	private final SolrFieldService solrFieldService;
	private final SolrFilterService solrFilterService;

	//maximum time allowed for a solr search in s
	//TODO application.properties this
	private static final int TIMEALLOWED = 30;

	private Logger log = LoggerFactory.getLogger(getClass());
	
	public SolrSampleService(SolrSampleRepository solrSampleRepository,
							 BioSamplesProperties bioSamplesProperties,
							 SolrFacetService solrFacetService,
							 SolrFieldService solrFieldService,
							 SolrFilterService solrFilterService) {
		this.solrSampleRepository = solrSampleRepository;
		this.bioSamplesProperties = bioSamplesProperties;
		this.solrFacetService = solrFacetService;
		this.solrFieldService = solrFieldService;
		this.solrFilterService = solrFilterService;
	}

	public Page<SolrSample> fetchSolrSampleByText(String searchTerm, MultiValueMap<String,String> filters, 
			Collection<String> domains, Instant after, Instant before, Pageable pageable) {
		//default to search all
		if (searchTerm == null || searchTerm.trim().length() == 0) {
			searchTerm = "*:*";
		}
		//build a query out of the users string and any facets
		Query query = new SimpleQuery(searchTerm);
		query.setPageRequest(pageable);
				
		if (filters != null) {
			query = addFilters(query, filters);
		}		

		//filter out non-public
		//filter to update date range
		FilterQuery filterQuery = new SimpleFilterQuery();
		//check if this is a read superuser
		if (!domains.contains(bioSamplesProperties.getBiosamplesAapSuperRead())) {
			//user can only see private samples inside its own domain
			filterQuery.addCriteria(new Criteria("release_dt").lessThan("NOW").and("release_dt").isNotNull()
					.or(new Criteria("domain_s").in(domains)));
		}
		if (after != null && before != null) {
			filterQuery.addCriteria(new Criteria("update_dt").between(DateTimeFormatter.ISO_INSTANT.format(after), DateTimeFormatter.ISO_INSTANT.format(before)));
		} else if (after == null && before != null) {
			filterQuery.addCriteria(new Criteria("update_dt").between("NOW-1000YEAR", DateTimeFormatter.ISO_INSTANT.format(before)));
		} else if (after != null && before == null) {
			filterQuery.addCriteria(new Criteria("update_dt").between(DateTimeFormatter.ISO_INSTANT.format(after), "NOW+1000YEAR"));
		}
		query.addFilterQuery(filterQuery);
		query.setTimeAllowed(TIMEALLOWED*1000); 
		
		// return the samples from solr that match the query
		return solrSampleRepository.findByQuery(query);
	}

	public Page<SolrSample> fetchSolrSampleByText(String searchTerm, Collection<Filter> filters, Collection<String> domains, Pageable pageable) {

		//default to search all
		if (searchTerm == null || searchTerm.trim().length() == 0) {
			searchTerm = "*:*";
		}
		//build a query out of the users string and any facets
		Query query = new SimpleQuery(searchTerm);
		query.setPageRequest(pageable);

		Optional<FilterQuery> optionalFilter = solrFilterService.getFilterQuery(filters);
		optionalFilter.ifPresent(query::addFilterQuery);

		FilterQuery publicFilterQuery = solrFilterService.getPublicFilterQuery(domains);
		query.addFilterQuery(publicFilterQuery);
		query.setTimeAllowed(TIMEALLOWED*1000);

		// return the samples from solr that match the query
		return solrSampleRepository.findByQuery(query);

/*
//		query.addFilterQuery(filterQuery);
//		if (filters != null) {
//			query = addFilters(query, filters);
//		}

//		//filter out non-public
//		//filter to update date range
//		FilterQuery filterQuery = new SimpleFilterQuery();
//		//check if this is a read superuser
//		if (!domains.contains(bioSamplesProperties.getBiosamplesAapSuperRead())) {
//			//user can only see private samples inside its own domain
//			filterQuery.addCriteria(new Criteria("release_dt").lessThan("NOW").and("release_dt").isNotNull()
//					.or(new Criteria("domain_s").in(domains)));
//		}
//		TODO this has to be implemented as date filters and not as instants
//		if (after != null && before != null) {
//			filterQuery.addCriteria(new Criteria("update_dt").between(DateTimeFormatter.ISO_INSTANT.format(after), DateTimeFormatter.ISO_INSTANT.format(before)));
//		} else if (after == null && before != null) {
//			filterQuery.addCriteria(new Criteria("update_dt").between("NOW-1000YEAR", DateTimeFormatter.ISO_INSTANT.format(before)));
//		} else if (after != null && before == null) {
//			filterQuery.addCriteria(new Criteria("update_dt").between(DateTimeFormatter.ISO_INSTANT.format(after), "NOW+1000YEAR"));
//		}
 */
	}

//	public List<Facet> getFacets(String searchTerm, Collection<String> filters, Collection<String> domains, Pageable facetPageable, Pageable facetValuePageable) {
//
//		return solrFacetService.getFacets(searchTerm, filters, domains);
//
//	}

	public Autocomplete getAutocomplete(String autocompletePrefix, MultiValueMap<String,String> filters, int maxSuggestions) {
		//default to search all
		String searchTerm = "*:*";
		//build a query out of the users string and any facets
		FacetQuery query = new SimpleFacetQuery();
		query.addCriteria(new Criteria().expression(searchTerm));
		query.setPageRequest(new PageRequest(0, 1));
				
		if (filters != null) {
			query = addFilters(query, filters);
		}		

		//filter out non-public
		FilterQuery filterQuery = new SimpleFilterQuery();
		filterQuery.addCriteria(new Criteria("release_dt").lessThan("NOW").and("release_dt").isNotNull());
		query.addFilterQuery(filterQuery);

		FacetOptions facetOptions = new FacetOptions();
		facetOptions.addFacetOnField("autocomplete_ss");
		facetOptions.setPageable(new PageRequest(0, maxSuggestions));
		facetOptions.setFacetPrefix(autocompletePrefix);
		query.setFacetOptions(facetOptions);
		query.setTimeAllowed(TIMEALLOWED*1000); 
		
		FacetPage<?> facetPage = solrSampleRepository.findByFacetQuery(query);
		
		Page<FacetFieldEntry> facetFiledEntryPage = facetPage.getFacetResultPage("autocomplete_ss");
		
		List<String> autocompleted = new ArrayList<>();
		for (FacetFieldEntry facetFieldEntry : facetFiledEntryPage) {
			autocompleted.add(facetFieldEntry.getValue());
		}
		return new Autocomplete(autocompletePrefix, autocompleted);		
	}
	
	private <T extends Query> T addFilters(T query, MultiValueMap<String,String> filters) {
		//if no filters or filters are null, quick exit
		//TODO Update this part of the code to take into account how filters are handled
		if (filters == null || filters.size() == 0) {
			return query;
		}

		boolean filter = false;
		FilterQuery filterQuery = new SimpleFilterQuery();
		for (String facetType : filters.keySet()) {
			Criteria facetCriteria = null;

			String facetField = solrFieldService.encodeFieldName(facetType);
			for (String facatValue : filters.get(facetType)) {
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


//	private <T extends Query> T addFilters(T query, Collection<Filter> filters) {
//	    // TODO implement the method
//		if (filters == null || filters.size() == 0) {
//			return query;
//		}
//
//		boolean filterActive = false;
//		FilterQuery filterQuery = new SimpleFilterQuery();
//		for(Filter filter: filters) {
//			Optional<Criteria> optionalFilterCriteria = solrFilterService.getFilterCriteria(filter);
//			if (optionalFilterCriteria.isPresent()) {
//				filterQuery.addCriteria(optionalFilterCriteria.get());
//				filterActive = true;
//			}
//
//		}
//		if (filterActive) {
//			query.addFilterQuery(filterQuery);
//		}
//
//		return query;
//	}

//	/**
//	 * Build a filter criteria based on the filter type and filter content
//	 * @param filter
//	 * @return an optional solr criteria for filtering purpose
//	 */
//	private Optional<Criteria> buildCriteriaFromFilter(Filter filter) {
//	    //TODO implement the method
//		FilterContent content = filter.getContent();
//		FilterType type = filter.getKind();
//		String filterTargetField = solrFieldService.encodedField(filter.getLabel(), facetFilterConverter.convert(type));
//		Criteria filterCriteria = null;
//		if (content == null) {
//			 filterCriteria = new Criteria(filterTargetField).isNotNull();
//		} else {
//			switch(type) {
//				case ATTRIBUTE_FILTER:
//				case RELATION_FILER:
//				case INVERSE_RELATION_FILTER:
//					ValueFilter valueContent = (ValueFilter) content;
//					for(String value: valueContent.getContent()) {
//						if (filterCriteria == null) {
//							filterCriteria = new Criteria(filterTargetField).is(value);
//						} else {
//							filterCriteria = filterCriteria.or(new Criteria(filterTargetField).is(value));
//						}
//					}
//
//			}
//
//		}
//		return Optional.ofNullable(filterCriteria);
//
//	}
}
