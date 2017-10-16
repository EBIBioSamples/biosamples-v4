package uk.ac.ebi.biosamples.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.model.filters.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.TemporalAccessor;
import java.util.*;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;
import static java.time.format.DateTimeFormatter.ISO_LOCAL_TIME;


@Service
public class FilterService {

	private Logger log = LoggerFactory.getLogger(getClass());
	private final DateTimeFormatter formatter = new DateTimeFormatterBuilder()
			.parseCaseInsensitive()
			.append(ISO_LOCAL_DATE)
			.optionalStart()           // time made optional
			.appendLiteral('T')
			.append(ISO_LOCAL_TIME)
			.optionalStart()           // zone and offset made optional
			.appendOffsetId()
			.optionalStart()
			.appendLiteral('[')
			.parseCaseSensitive()
			.appendZoneRegionId()
			.appendLiteral(']')
			.optionalEnd()
			.optionalEnd()
			.optionalEnd()
			.toFormatter();

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
		String[] valueElements;
		//TODO code smell - Too many switch cases
		switch(filterType) {
			case ATTRIBUTE_FILTER:
			case RELATION_FILER:
			case INVERSE_RELATION_FILTER:
				valueElements = serializedValue.split(":", 2);
				filterLabel = valueElements[0];
				if(valueElements.length > 1) {
					List<String> listContent = new ArrayList<>();
					listContent.add(valueElements[1]);
					filterContent = new ValueFilter(listContent);
				}
				break;
			case DATE_FILTER:
				// TODO FilterService should know anything about how to do this, should be part of the filter class
                // TODO the method needs to be refactor
				valueElements = serializedValue.split(":",  2);
				filterLabel = valueElements[0];
				String filterValue = valueElements[1];
				ZonedDateTime from = null;
				ZonedDateTime to = null;
				int fromIndex = filterValue.indexOf("from:");
				int toIndex = filterValue.indexOf("to:");
				if (toIndex != -1) {
					if (fromIndex != -1) {
						from = parseDateTime(filterValue.substring(fromIndex + 5, toIndex));
					}
					to = parseDateTime(filterValue.substring(toIndex + 3));
				} else {
					if (fromIndex != -1)
						from = parseDateTime(filterValue.substring(fromIndex + 5));
				}
                filterContent = new DateRangeFilterContent(from, to);
				break;

		}

		return new Filter(filterType, filterLabel, filterContent);
	}

	private ZonedDateTime parseDateTime(String datetime) {
		TemporalAccessor temporalAccessor = formatter.parseBest(datetime,
				ZonedDateTime::from, LocalDateTime::from, LocalDate::from);
		if (temporalAccessor instanceof ZonedDateTime) {
			return (ZonedDateTime) temporalAccessor;
		} else if (temporalAccessor instanceof LocalDateTime) {
			return ((LocalDateTime) temporalAccessor).atZone(ZoneId.of("UTC"));
		} else {
			return ((LocalDate) temporalAccessor).atStartOfDay(ZoneId.of("UTC"));
		}

	}


}
