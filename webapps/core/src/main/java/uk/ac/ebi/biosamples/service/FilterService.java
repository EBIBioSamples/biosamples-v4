package uk.ac.ebi.biosamples.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.model.filters.*;

import java.util.*;


@Service
public class FilterService {

	private Logger log = LoggerFactory.getLogger(getClass());

	private final FilterFactory filterFactory;

	public FilterService(FilterFactory filterFactory) {
		this.filterFactory = filterFactory;
	}


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
			Filter filter = filterFactory.parseFilterFromString(filterString);

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




}
