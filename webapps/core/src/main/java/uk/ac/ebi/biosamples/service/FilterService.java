package uk.ac.ebi.biosamples.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.model.filters.*;

import java.util.*;


@Service
public class FilterService {

	private Logger log = LoggerFactory.getLogger(getClass());
	
	/**
	 * Converts an array of serialized filters to the corresponding collection of object
	 * @param filterStrings an array of serialized filters
	 * @return
	 */
	public Collection<Filter> getFiltersCollection(String[] filterStrings) {
		List<Filter> outputFilters = new ArrayList<>();
		if (filterStrings == null) return outputFilters;
		if (filterStrings.length == 0) return outputFilters;


		/*
		 *	For every filter I need to extract:
		 *	1. The kind of the filter
		 *  2. Label (which will be used to get the corresponding field in solr, so here is decoded)
		 *  3. The value
 		 */
		Arrays.sort(filterStrings);
		SortedSet<String> filterStringSet = new TreeSet<>(Arrays.asList(filterStrings));
		for(String filterString: filterStringSet) {
			FilterType filterType = FilterType.ofFilterString(filterString);
			String filterValue = filterString.replace(filterType.getSerialization() + ":","");
			Filter filter = getFilter(filterValue, filterType);

			/*
			 * If there's already a compatible filter in the list
			 * merge the two contents
			 * TODO Improvable?
             */
			int compatibleFilterIndex = -1;
			Filter newFilter = filter;
            for(int i=0; i<outputFilters.size(); i++) {
            	Filter outputFilter = outputFilters.get(i);
            	if(outputFilter.isCompatible(filter)) {
            		outputFilter.getContent().merge(filter.getContent());
					newFilter = outputFilter;
					compatibleFilterIndex = i;
					break;
				}
			}
			if (compatibleFilterIndex < 0) {
            	outputFilters.add(filter);
			} else {
            	outputFilters.set(compatibleFilterIndex, newFilter);
			}
		}

		return outputFilters;

	}

	/**
	 * Generate a Filter based on the provided serialized filter content and filter type
	 * @param serializedValue the content of the filter serialized
	 * @param filterType the kind of filter
	 * @return a new Filter
	 */
	private Filter getFilter(String serializedValue, FilterType filterType) {
		String filterLabel = "";
		FilterContent filterContent = new EmptyFilter();
		//TODO code smell - Too many switch cases
		switch(filterType) {
			case ATTRIBUTE_FILTER:
			case RELATION_FILER:
			case INVERSE_RELATION_FILTER:
				String[] valueElements = serializedValue.split(":", 2);
				filterLabel = valueElements[0];
				if(valueElements.length > 1) {
					List<String> listContent = new ArrayList<>();
					listContent.add(valueElements[1]);
					filterContent = new ValueFilter(listContent);
				}

		}

		return new Filter(filterType, filterLabel, filterContent);
	}


}
