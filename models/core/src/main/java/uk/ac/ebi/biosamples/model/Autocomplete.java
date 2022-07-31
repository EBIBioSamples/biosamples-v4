/*
* Copyright 2021 EMBL - European Bioinformatics Institute
* Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
* file except in compliance with the License. You may obtain a copy of the License at
* http://www.apache.org/licenses/LICENSE-2.0
* Unless required by applicable law or agreed to in writing, software distributed under the
* License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
* CONDITIONS OF ANY KIND, either express or implied. See the License for the
* specific language governing permissions and limitations under the License.
*/
package uk.ac.ebi.biosamples.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class Autocomplete {

  private final String query;
  private final List<String> suggestions;

  public Autocomplete(String query, List<String> suggestions) {
    // setup defaults to avoid nulls
    if (query == null) {
      query = "";
    }
    if (suggestions == null) {
      suggestions = new ArrayList<>();
    }

    // store the query used
    this.query = query;

    // store the suggestions as an unmodifiable list so it can't be changed by accident
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
  public static Autocomplete build(
      @JsonProperty("query") String query, @JsonProperty("suggestions") List<String> suggestions) {
    if (query == null) {
      query = "";
    }
    if (suggestions == null) {
      suggestions = new LinkedList<>();
    }
    return new Autocomplete(query, suggestions);
  }
}
