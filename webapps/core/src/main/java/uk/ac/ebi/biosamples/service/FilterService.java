package uk.ac.ebi.biosamples.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriUtils;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.SortedSet;
import java.util.TreeSet;


@Service
public class FilterService {

	private Logger log = LoggerFactory.getLogger(getClass());
	
	public MultiValueMap<String,String> getFilters(String[] filterStrings) {
		if (filterStrings == null) return new LinkedMultiValueMap<>();
		if (filterStrings.length == 0) return new LinkedMultiValueMap<>();
		//sort the array
		Arrays.sort(filterStrings);
		SortedSet<String> filterStringSet = new TreeSet<>(Arrays.asList(filterStrings));
		//strip the requestParams down to just the selected facet information
		MultiValueMap<String, String> filters = new LinkedMultiValueMap<>();
		for (String filterString : filterStringSet) {
			log.info("looking at filter string '" + filterString + "'");
			if (filterString.contains(":")) {
				// Assume filter format is FacetType:FacetLabel:FacetLabelValue

				String[] filterParts = filterString.split(":", 3);
				String key = filterParts[0] + ":" + filterParts[1];
				String value = null;
				if (filterParts.length > 2) {
					value = filterParts[2];
				}
				filters.add(decodeParam(key), decodeParam(value));
				log.info("adding filter " + key + " = " + value);
			}
		}
		return filters;
	}

	private String encodeParam(String queryParam) {
		try {
			return UriUtils.encodeQueryParam(queryParam, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	private String decodeParam(String queryParam) {
		try {
			return UriUtils.decode(queryParam, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}


}
