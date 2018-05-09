package uk.ac.ebi.biosamples.utils;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;

import uk.ac.ebi.biosamples.model.filter.DateRangeFilter;
import uk.ac.ebi.biosamples.model.filter.Filter;

public class ArgUtils {
	private static Logger log = LoggerFactory.getLogger(ArgUtils.class);
	
	public static Collection<Filter> getDateFilters(ApplicationArguments args) {
		
		LocalDate fromDate = null;
		if (args.getOptionNames().contains("from")) {
			fromDate = LocalDate.parse(args.getOptionValues("from").iterator().next(),
					DateTimeFormatter.ISO_LOCAL_DATE);
		} else {
			fromDate = LocalDate.parse("1000-01-01", DateTimeFormatter.ISO_LOCAL_DATE);
		}
		LocalDate toDate = null;
		if (args.getOptionNames().contains("until")) {
			toDate = LocalDate.parse(args.getOptionValues("until").iterator().next(), DateTimeFormatter.ISO_LOCAL_DATE);
		} else {
			toDate = LocalDate.parse("3000-01-01", DateTimeFormatter.ISO_LOCAL_DATE);
		}
		
		log.info("Processing samples from "+DateTimeFormatter.ISO_LOCAL_DATE.format(fromDate));
		log.info("Processing samples to "+DateTimeFormatter.ISO_LOCAL_DATE.format(toDate));
		
		Filter fromDateFilter = new DateRangeFilter.DateRangeFilterBuilder("update")
				.from(fromDate.atStartOfDay().toInstant(ZoneOffset.UTC))
				.until(toDate.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC))
				.build();
		Collection<Filter> filters = new ArrayList<>();
		filters.add(fromDateFilter);
		return filters;
	}
}
