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
package uk.ac.ebi.biosamples.core.model;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import uk.ac.ebi.biosamples.core.model.filter.DateRangeFilter;
import uk.ac.ebi.biosamples.core.model.filter.Filter;

@Getter
public class SampleAnalytics {
  private long totalRecords;
  private String dateRange;
  private long processedRecords;
  protected Map<String, Long> center;
  protected Map<String, Long> channel;
  protected Map<String, Map<String, Long>> facets;

  public SampleAnalytics() {
    center = new HashMap<>();
    channel = new HashMap<>();
    facets = new HashMap<>();
  }

  public void setTotalRecords(final long totalRecords) {
    this.totalRecords = totalRecords;
  }

  public void setDateRange(final String dateRange) {
    this.dateRange = dateRange;
  }

  public void setDateRange(final Collection<Filter> filters) {
    final DateRangeFilter dateRangeFilter =
        filters.stream()
            .filter(f -> f instanceof DateRangeFilter)
            .map(DateRangeFilter.class::cast)
            .findFirst()
            .orElse(null);
    if (dateRangeFilter != null && dateRangeFilter.getContent().isPresent()) {
      final DateTimeFormatter dateTimeFormatter =
          DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.systemDefault());
      dateRange =
          dateTimeFormatter.format(dateRangeFilter.getContent().get().getFrom())
              + " : "
              + dateTimeFormatter.format(dateRangeFilter.getContent().get().getUntil());
    }
  }

  public void setProcessedRecords(final long processedRecords) {
    this.processedRecords = processedRecords;
  }

  public void addToCenter(final String centerName) {
    if (center.containsKey(centerName)) {
      center.put(centerName, center.get(centerName) + 1);
    } else {
      center.put(centerName, 1L);
    }
  }

  public void addToChannel(final String accessionPrefix) {
    if (channel.containsKey(accessionPrefix)) {
      channel.put(accessionPrefix, channel.get(accessionPrefix) + 1);
    } else {
      channel.put(accessionPrefix, 1L);
    }
  }

  public void addToFacets() {}
}
