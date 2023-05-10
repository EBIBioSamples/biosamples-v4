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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import uk.ac.ebi.biosamples.model.PipelineName;
import uk.ac.ebi.biosamples.model.filter.AttributeFilter;
import uk.ac.ebi.biosamples.model.filter.DateRangeFilter;
import uk.ac.ebi.biosamples.model.filter.Filter;

public class PipelineUtils {
  private static final Logger log = LoggerFactory.getLogger(PipelineUtils.class);

  public static Collection<Filter> getDateFilters(
      final ApplicationArguments args, final String dateType) {
    final Collection<Filter> filters = new ArrayList<>();
    final LocalDate fromDate;
    final LocalDate toDate;
    final Filter fromDateFilter;

    if (args.getOptionNames().contains("from")) {
      fromDate =
          LocalDate.parse(
              args.getOptionValues("from").iterator().next(), DateTimeFormatter.ISO_LOCAL_DATE);
    } else {
      fromDate = LocalDate.parse("1000-01-01", DateTimeFormatter.ISO_LOCAL_DATE);
    }

    if (args.getOptionNames().contains("until")) {
      toDate =
          LocalDate.parse(
              args.getOptionValues("until").iterator().next(), DateTimeFormatter.ISO_LOCAL_DATE);
    } else {
      toDate = LocalDate.parse("3000-01-01", DateTimeFormatter.ISO_LOCAL_DATE);
    }

    log.info("Processing samples from " + DateTimeFormatter.ISO_LOCAL_DATE.format(fromDate));
    log.info("Processing samples to " + DateTimeFormatter.ISO_LOCAL_DATE.format(toDate));

    if (!dateType.equals("release")) {
      fromDateFilter =
          new DateRangeFilter.DateRangeFilterBuilder(dateType)
              .from(fromDate.atStartOfDay().toInstant(ZoneOffset.UTC))
              .until(toDate.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC))
              .build();
    } else {
      fromDateFilter =
          new DateRangeFilter.DateRangeFilterBuilder(dateType)
              .from(fromDate.atStartOfDay().toInstant(ZoneOffset.UTC))
              .until(fromDate.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC))
              .build();
    }

    filters.add(fromDateFilter);

    return filters;
  }

  public static void writeFailedSamplesToFile(
      final Map<String, String> failures, final PipelineName pipelineName) {
    BufferedWriter bf = null;
    final File file = new File(pipelineName + ".failed");

    try {
      bf = new BufferedWriter(new FileWriter(file));
      for (final Map.Entry<String, String> entry : failures.entrySet()) {
        bf.write(entry.getKey() + " : " + entry.getValue());
        bf.newLine();
      }

      bf.flush();
    } catch (final IOException e) {
      log.info("Failed to write failed file ", e);
    } finally {
      try {
        assert bf != null;
        bf.close();
      } catch (final Exception e) {
        log.error(e.getMessage(), e);
      }
    }
  }

  public static void exitPipeline(final ConfigurableApplicationContext ctx) {
    final int exitCode = SpringApplication.exit(ctx, () -> 0);
    log.info("Exit Spring Boot");

    System.exit(exitCode);
  }

  public static Filter getAttributeFilter(final String attributeName, final String attributeValue) {
    return new AttributeFilter.Builder(attributeName).withValue(attributeValue).build();
  }
}
