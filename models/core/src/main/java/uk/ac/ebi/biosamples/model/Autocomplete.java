package uk.ac.ebi.biosamples.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Autocomplete {

	private final String query;
	private final List<String> suggestions;
	
	public Autocomplete(String query, List<String> suggestions) {
		if (query == null) {
			query = "";
		}
		if (suggestions == null) {
			suggestions = new ArrayList<>();
		}
		
		this.query = query;
		List<String> wrappedSuggestions = new ArrayList<>();
		wrappedSuggestions.addAll(suggestions);
		this.suggestions = Collections.unmodifiableList(wrappedSuggestions);
	}

	public String getQuery() {
		return query;
	}

	public List<String> getSuggestions() {
		return suggestions;
	}
}
