package uk.ac.ebi.biosamples.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Autocomplete {

	private final String query;
	private final List<String> suggestions;
	
	public Autocomplete(String query, List<String> suggestions) {
		//setup defaults to avoid nulls
		if (query == null) {
			query = "";
		}
		if (suggestions == null) {
			suggestions = new ArrayList<>();
		}
		
		//store the query used
		this.query = query;
		
		//store the suggestions as an unmodifiable list so it can't be changed by accident
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
	
	@JsonCreator
	public static Autocomplete build(@JsonProperty("query") String query, @JsonProperty("suggestions") List<String> suggestions) {
		if (query == null) {
			query = "";
		}			
		if (suggestions == null) {
			suggestions = new LinkedList<>();
		}
		return new Autocomplete(query, suggestions);
	}
}
