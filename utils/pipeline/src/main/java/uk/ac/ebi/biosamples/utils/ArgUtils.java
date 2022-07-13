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

    LocalDate fromDate;
    if (args.getOptionNames().contains("from")) {
      fromDate =
          LocalDate.parse(
              args.getOptionValues("from").iterator().next(), DateTimeFormatter.ISO_LOCAL_DATE);
    } else {
      fromDate = LocalDate.parse("1000-01-01", DateTimeFormatter.ISO_LOCAL_DATE);
    }
    LocalDate toDate;
    if (args.getOptionNames().contains("until")) {
      toDate =
          LocalDate.parse(
              args.getOptionValues("until").iterator().next(), DateTimeFormatter.ISO_LOCAL_DATE);
    } else {
      toDate = LocalDate.parse("3000-01-01", DateTimeFormatter.ISO_LOCAL_DATE);
    }

    log.info("Processing samples from " + DateTimeFormatter.ISO_LOCAL_DATE.format(fromDate));
    log.info("Processing samples to " + DateTimeFormatter.ISO_LOCAL_DATE.format(toDate));

    Filter fromDateFilter =
        new DateRangeFilter.DateRangeFilterBuilder("update")
            .from(fromDate.atStartOfDay().toInstant(ZoneOffset.UTC))
            .until(toDate.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC))
            .build();
    Collection<Filter> filters = new ArrayList<>();
    filters.add(fromDateFilter);
    return filters;
  }
}
