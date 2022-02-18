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
package uk.ac.ebi.biosamples.curation;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.hateoas.Resource;
import org.springframework.stereotype.Component;
import uk.ac.ebi.biosamples.PipelineFutureCallback;
import uk.ac.ebi.biosamples.PipelineResult;
import uk.ac.ebi.biosamples.PipelinesProperties;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.PipelineAnalytics;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.SampleAnalytics;
import uk.ac.ebi.biosamples.model.filter.AttributeFilter;
import uk.ac.ebi.biosamples.model.filter.Filter;
import uk.ac.ebi.biosamples.mongo.model.MongoSample;
import uk.ac.ebi.biosamples.mongo.repo.MongoCurationRuleRepository;
import uk.ac.ebi.biosamples.mongo.repo.MongoSampleRepository;
import uk.ac.ebi.biosamples.mongo.repo.MongoStructuredDataRepository;
import uk.ac.ebi.biosamples.service.AnalyticsService;
import uk.ac.ebi.biosamples.utils.AdaptiveThreadPoolExecutor;
import uk.ac.ebi.biosamples.utils.ArgUtils;
import uk.ac.ebi.biosamples.utils.MailSender;
import uk.ac.ebi.biosamples.utils.ThreadUtils;

@Component
public class MigrationApplicationRunner implements ApplicationRunner {
  private static final Logger LOG = LoggerFactory.getLogger(MigrationApplicationRunner.class);

  private final BioSamplesClient bioSamplesClient;
  private final PipelinesProperties pipelinesProperties;
  private final PipelineFutureCallback pipelineFutureCallback;
  private final AnalyticsService analyticsService;
  private final MongoSampleRepository mongoSampleRepository;
  private final MongoStructuredDataRepository mongoStructuredDataRepository;

  public MigrationApplicationRunner(
      BioSamplesClient bioSamplesClient,
      PipelinesProperties pipelinesProperties,
      AnalyticsService analyticsService,
      MongoSampleRepository mongoSampleRepository,
      MongoStructuredDataRepository mongoStructuredDataRepository) {
    this.bioSamplesClient = bioSamplesClient;
    this.pipelinesProperties = pipelinesProperties;
    this.pipelineFutureCallback = new PipelineFutureCallback();
    this.analyticsService = analyticsService;
    this.mongoSampleRepository = mongoSampleRepository;
    this.mongoStructuredDataRepository = mongoStructuredDataRepository;
  }

  @Override
  public void run(ApplicationArguments args) throws Exception {
    Collection<Filter> filters = ArgUtils.getDateFilters(args);
    Instant startTime = Instant.now();
    LOG.info("Pipeline started at {}", startTime);
    long sampleCount = 0;
    boolean isPassed = true;
    SampleAnalytics sampleAnalytics = new SampleAnalytics();

    try (AdaptiveThreadPoolExecutor executorService =
             AdaptiveThreadPoolExecutor.create(
                 100,
                 10000,
                 true,
                 pipelinesProperties.getThreadCount(),
                 pipelinesProperties.getThreadCountMax())) {

      Map<String, Future<PipelineResult>> futures = new HashMap<>();
      for (Resource<Sample> sampleResource : bioSamplesClient.fetchSampleResourceAll("", filters)) {
        LOG.trace("Handling {}", sampleResource);
        Sample sample = sampleResource.getContent();
        Objects.requireNonNull(sample);

        if (sample.getData() != null && !sample.getData().isEmpty()) {
          LOG.info("Migrating structured data of sample: {}", sample.getAccession());
          MongoSample mongoSample = mongoSampleRepository.findOne(sample.getAccession());
          Callable<PipelineResult> task =
              new MigrationCallable(
                  mongoSample, mongoSampleRepository, mongoStructuredDataRepository);
          futures.put(mongoSample.getAccession(), executorService.submit(task));
        }

        if (++sampleCount % 5000 == 0) {
          LOG.info("Scheduled sample count {}", sampleCount);
        }
      }

      LOG.info("Waiting for all scheduled tasks to finish");
      ThreadUtils.checkAndCallbackFutures(futures, 0, pipelineFutureCallback);
    } catch (final Exception e) {
      LOG.error("Pipeline failed to finish successfully", e);
      isPassed = false;
      throw e;
    } finally {
      Instant endTime = Instant.now();
      LOG.info("Total samples processed {}", sampleCount);
      LOG.info("Total data objects migrated {}", pipelineFutureCallback.getTotalCount());
      LOG.info("Pipeline finished at {}", endTime);
      LOG.info(
          "Pipeline total running time {} seconds",
          Duration.between(startTime, endTime).getSeconds());

      PipelineAnalytics pipelineAnalytics =
          new PipelineAnalytics(
              "StructuredDataMigration", startTime, endTime, sampleCount, pipelineFutureCallback.getTotalCount());
      pipelineAnalytics.setDateRange(filters);
      sampleAnalytics.setDateRange(filters);
      sampleAnalytics.setProcessedRecords(sampleCount);
      analyticsService.persistSampleAnalytics(startTime, sampleAnalytics);
      analyticsService.persistPipelineAnalytics(pipelineAnalytics);
      MailSender.sendEmail("StructuredDataMigration", handleFailedSamples(), isPassed);
    }
  }

  private String handleFailedSamples() {
    final ConcurrentLinkedQueue<String> failedQueue = MigrationCallable.failedQueue;
    String failures = null;
    if (!failedQueue.isEmpty()) {
      List<String> fails = new LinkedList<>();
      while (failedQueue.peek() != null) {
        fails.add(failedQueue.poll());
      }
      failures = "Failed files (" + fails.size() + ") " + String.join(" , ", fails);
      LOG.warn(failures);
    } else {
      LOG.info("Pipeline completed without any failures");
    }
    return failures;
  }
}
