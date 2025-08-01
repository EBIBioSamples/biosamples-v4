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
package uk.ac.ebi.biosamples.mongo.service;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.core.model.PipelineAnalytics;
import uk.ac.ebi.biosamples.core.model.SampleAnalytics;
import uk.ac.ebi.biosamples.mongo.model.MongoAnalytics;
import uk.ac.ebi.biosamples.mongo.repository.MongoAnalyticsRepository;

@Service
public class AnalyticsService {
  private static final Logger LOG = LoggerFactory.getLogger(AnalyticsService.class);
  private final MongoAnalyticsRepository analyticsRepository;
  private final DateTimeFormatter dateTimeFormatter;

  public AnalyticsService(final MongoAnalyticsRepository analyticsRepository) {
    this.analyticsRepository = analyticsRepository;
    dateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");
  }

  public MongoAnalytics getAnalytics(final LocalDate date) {
    final String dateString = dateTimeFormatter.format(date);
    final Optional<MongoAnalytics> byId = analyticsRepository.findById(dateString);

    return byId.isPresent() ? byId.get() : null;
  }

  public List<MongoAnalytics> getAnalytics(final LocalDate start, final LocalDate end) {
    final String startString = dateTimeFormatter.format(start);
    final String endString = dateTimeFormatter.format(end);

    return analyticsRepository.findMongoAnalyticsByIdBetween(startString, endString);
  }

  public MongoAnalytics getLatestAnalytics() {
    return analyticsRepository.findFirstByOrderByCollectionDateDesc();
  }

  public void mergeSampleAnalytics(final Instant runTime, final SampleAnalytics sampleAnalytics) {
    final String pipelineRunDate = getApproximateRunDateAsString(runTime);
    LOG.info("Saving sample types for date: {}", pipelineRunDate);

    final LocalDate localDate = runTime.atZone(ZoneId.systemDefault()).toLocalDate();
    //    LocalDate localDate = LocalDate.ofInstant(runTime, ZoneId.systemDefault());
    final Optional<MongoAnalytics> byId = analyticsRepository.findById(pipelineRunDate);
    MongoAnalytics analyticsRecord = byId.isPresent() ? byId.get() : null;

    if (analyticsRecord == null) {
      analyticsRecord =
          new MongoAnalytics(
              pipelineRunDate,
              localDate.getYear(),
              localDate.getMonthValue(),
              localDate.getDayOfMonth());
    } else {
      final SampleAnalytics oldSampleAnalytics = analyticsRecord.getSampleAnalytics();
      if (oldSampleAnalytics != null) {
        sampleAnalytics.getCenter().putAll(oldSampleAnalytics.getCenter());
        sampleAnalytics.getChannel().putAll(oldSampleAnalytics.getChannel());
        sampleAnalytics.setProcessedRecords(oldSampleAnalytics.getProcessedRecords());
        sampleAnalytics.setDateRange(oldSampleAnalytics.getDateRange());
      }
    }

    analyticsRecord.setSampleAnalytics(sampleAnalytics);
    analyticsRepository.save(analyticsRecord);
  }

  public void persistSampleAnalytics(final Instant runTime, final SampleAnalytics sampleAnalytics) {
    final String pipelineRunDate = getApproximateRunDateAsString(runTime);
    LOG.info("Saving sample types for date: {}", pipelineRunDate);

    final LocalDate localDate = runTime.atZone(ZoneId.systemDefault()).toLocalDate();
    final Optional<MongoAnalytics> byId = analyticsRepository.findById(pipelineRunDate);
    MongoAnalytics analyticsRecord = byId.isPresent() ? byId.get() : null;

    if (analyticsRecord == null) {
      analyticsRecord =
          new MongoAnalytics(
              pipelineRunDate,
              localDate.getYear(),
              localDate.getMonthValue(),
              localDate.getDayOfMonth());
    }
    analyticsRecord.setSampleAnalytics(sampleAnalytics);
    analyticsRepository.save(analyticsRecord);
  }

  public void persistPipelineAnalytics(final PipelineAnalytics pipelineAnalytics) {
    final String pipelineRunDate = getApproximateRunDateAsString(pipelineAnalytics.getStartTime());
    LOG.info("Saving {} analytics for date: {}", pipelineAnalytics.getName(), pipelineRunDate);

    final LocalDate localDate =
        pipelineAnalytics.getStartTime().atZone(ZoneId.systemDefault()).toLocalDate();
    final Optional<MongoAnalytics> byId = analyticsRepository.findById(pipelineRunDate);
    MongoAnalytics analyticsRecord = byId.isPresent() ? byId.get() : null;

    if (analyticsRecord == null) {
      analyticsRecord =
          new MongoAnalytics(
              pipelineRunDate,
              localDate.getYear(),
              localDate.getMonthValue(),
              localDate.getDayOfMonth());
    }
    analyticsRecord.addPipelineAnalytics(pipelineAnalytics);
    analyticsRepository.save(analyticsRecord);
  }

  private String getApproximateRunDateAsString(final Instant date) {
    final LocalDateTime adjustedDate = LocalDateTime.ofInstant(date, ZoneOffset.UTC).plusHours(3);

    return adjustedDate.format(dateTimeFormatter);
  }
}
