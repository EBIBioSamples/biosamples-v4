package uk.ac.ebi.biosamples.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.model.PipelineAnalytics;
import uk.ac.ebi.biosamples.model.SampleAnalytics;
import uk.ac.ebi.biosamples.mongo.model.MongoAnalytics;
import uk.ac.ebi.biosamples.mongo.repo.MongoAnalyticsRepository;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class AnalyticsService {
    private static final Logger LOG = LoggerFactory.getLogger(AnalyticsService.class);

    private final MongoAnalyticsRepository analyticsRepository;
    private final DateTimeFormatter dateTimeFormatter;

    public AnalyticsService(MongoAnalyticsRepository analyticsRepository) {
        this.analyticsRepository = analyticsRepository;
        this.dateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");
    }

    public MongoAnalytics getAnalytics(LocalDate date) {
        String dateString = dateTimeFormatter.format(date);
        return analyticsRepository.findOne(dateString);
    }

    public List<MongoAnalytics> getAnalytics(LocalDate start, LocalDate end) {
        String startString = dateTimeFormatter.format(start);
        String endString = dateTimeFormatter.format(end);
        return analyticsRepository.findMongoAnalyticsByIdBetween(startString, endString);
    }

    public void persistSampleAnalytics(Instant runTime, SampleAnalytics sampleAnalytics) {
        String pipelineRunDate = getApproximateRunDateAsString(runTime);
        LOG.info("Saving sample types for date: {}", pipelineRunDate);

        MongoAnalytics analyticsRecord = analyticsRepository.findOne(pipelineRunDate);
        if (analyticsRecord == null) {
            analyticsRecord = new MongoAnalytics(pipelineRunDate);
        }
        analyticsRecord.setSampleAnalytics(sampleAnalytics);
        analyticsRepository.save(analyticsRecord);
    }

    public void persistPipelineAnalytics(PipelineAnalytics pipelineAnalytics) {
        String pipelineRunDate = getApproximateRunDateAsString(pipelineAnalytics.getStartTime());
        LOG.info("Saving {} analytics for date: {}", pipelineAnalytics.getName(), pipelineRunDate);

        MongoAnalytics analyticsRecord = analyticsRepository.findOne(pipelineRunDate);
        if (analyticsRecord == null) {
            analyticsRecord = new MongoAnalytics(pipelineRunDate);
        }
        analyticsRecord.addPipelineAnalytics(pipelineAnalytics);
        analyticsRepository.save(analyticsRecord);
    }

    public String getApproximateRunDateAsString(Instant date) {
        LocalDateTime adjustedDate = LocalDateTime.ofInstant(date, ZoneOffset.UTC).plusHours(3);
        return adjustedDate.format(dateTimeFormatter);
    }
}
