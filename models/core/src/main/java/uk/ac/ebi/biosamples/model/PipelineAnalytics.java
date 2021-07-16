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

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import uk.ac.ebi.biosamples.model.filter.DateRangeFilter;
import uk.ac.ebi.biosamples.model.filter.Filter;

public class PipelineAnalytics {
  private String name;
  private Instant startTime;
  private Instant endTime;
  private String dateRange;
  private long processedRecords;
  private long modifiedRecords;

  public PipelineAnalytics(
      String name,
      Instant startTime,
      Instant endTime,
      long processedRecords,
      long modifiedRecords) {
    this.name = name;
    this.startTime = startTime;
    this.endTime = endTime;
    this.processedRecords = processedRecords;
    this.modifiedRecords = modifiedRecords;
  }

  public Instant getStartTime() {
    return startTime;
  }

  public void setStartTime(Instant startTime) {
    this.startTime = startTime;
  }

  public Instant getEndTime() {
    return endTime;
  }

  public void setEndTime(Instant endTime) {
    this.endTime = endTime;
  }

  public String getDateRange() {
    return dateRange;
  }

  public void setDateRange(String dateRange) {
    this.dateRange = dateRange;
  }

  public void setDateRange(Collection<Filter> filters) {
    DateRangeFilter dateRangeFilter =
        filters.stream()
            .filter(f -> f instanceof DateRangeFilter)
            .map(DateRangeFilter.class::cast)
            .findFirst()
            .orElse(null);
    if (dateRangeFilter != null && dateRangeFilter.getContent().isPresent()) {
      DateTimeFormatter dateTimeFormatter =
          DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.systemDefault());
      this.dateRange =
          dateTimeFormatter.format(dateRangeFilter.getContent().get().getFrom())
              + " : "
              + dateTimeFormatter.format(dateRangeFilter.getContent().get().getUntil());
    }
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public long getProcessedRecords() {
    return processedRecords;
  }

  public void setProcessedRecords(long processedRecords) {
    this.processedRecords = processedRecords;
  }

  public long getModifiedRecords() {
    return modifiedRecords;
  }

  public void setModifiedRecords(long modifiedRecords) {
    this.modifiedRecords = modifiedRecords;
  }
}
