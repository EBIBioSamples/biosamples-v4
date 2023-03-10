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
package uk.ac.ebi.biosamples.service;

import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.model.filter.Filter;

@Service
public class FilterService {

  private final Logger log = LoggerFactory.getLogger(getClass());

  /**
   * Converts an array of serialized filters to the corresponding collection of object
   *
   * @param filterStrings an array of serialized filters
   * @return
   */
  public Collection<Filter> getFiltersCollection(final String[] filterStrings) {
    final List<Filter> outputFilters = new ArrayList<>();
    if (filterStrings == null) {
      return outputFilters;
    }
    if (filterStrings.length == 0) {
      return outputFilters;
    }

    /*
     *	For every filter I need to extract:
     *	1. The kind of the filter
     *  2. Label (which will be used to get the corresponding field in solr, so here is decoded)
     *  3. The value
     */
    Arrays.sort(filterStrings);
    final SortedSet<String> filterStringSet = new TreeSet<>(Arrays.asList(filterStrings));
    for (final String filterString : filterStringSet) {
      if (!filterString.isEmpty()) {
        outputFilters.add(FilterBuilder.create().buildFromString(filterString));
      }
    }

    return outputFilters;
  }
}
