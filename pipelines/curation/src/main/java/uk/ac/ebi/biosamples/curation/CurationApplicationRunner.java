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
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.hateoas.Resource;
import org.springframework.stereotype.Component;
import uk.ac.ebi.biosamples.PipelineFutureCallback;
import uk.ac.ebi.biosamples.PipelineResult;
import uk.ac.ebi.biosamples.PipelinesProperties;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.curation.service.IriUrlValidatorService;
import uk.ac.ebi.biosamples.model.PipelineAnalytics;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.filter.Filter;
import uk.ac.ebi.biosamples.ols.OlsProcessor;
import uk.ac.ebi.biosamples.service.CurationApplicationService;
import uk.ac.ebi.biosamples.utils.AdaptiveThreadPoolExecutor;
import uk.ac.ebi.biosamples.utils.ArgUtils;
import uk.ac.ebi.biosamples.utils.MailSender;
import uk.ac.ebi.biosamples.utils.ThreadUtils;
import uk.ac.ebi.biosamples.utils.mongo.AnalyticsService;

@Component
public class CurationApplicationRunner implements ApplicationRunner {
  private static final Logger LOG = LoggerFactory.getLogger(CurationApplicationRunner.class);
  private final BioSamplesClient bioSamplesClient;
  private final PipelinesProperties pipelinesProperties;
  private final OlsProcessor olsProcessor;
  private final CurationApplicationService curationApplicationService;
  private final AnalyticsService analyticsService;
  private final PipelineFutureCallback pipelineFutureCallback;

  @Autowired IriUrlValidatorService iriUrlValidatorService;

  public CurationApplicationRunner(
      @Qualifier("AAPCLIENT") BioSamplesClient bioSamplesClient,
      PipelinesProperties pipelinesProperties,
      OlsProcessor olsProcessor,
      CurationApplicationService curationApplicationService,
      AnalyticsService analyticsService) {
    this.bioSamplesClient = bioSamplesClient;
    this.pipelinesProperties = pipelinesProperties;
    this.olsProcessor = olsProcessor;
    this.curationApplicationService = curationApplicationService;
    this.analyticsService = analyticsService;
    this.pipelineFutureCallback = new PipelineFutureCallback();
  }

  @Override
  public void run(ApplicationArguments args) throws Exception {
    Instant startTime = Instant.now();
    Collection<Filter> filters = ArgUtils.getDateFilters(args);
    boolean isPassed = true;
    long sampleCount = 0;

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
        if (sample == null) {
          throw new RuntimeException("Sample should not be null");
        }

        Callable<PipelineResult> task =
            new SampleCurationCallable(
                bioSamplesClient,
                sample,
                olsProcessor,
                curationApplicationService,
                pipelinesProperties.getCurationDomain(),
                iriUrlValidatorService);
        sampleCount++;
        if (sampleCount % 10000 == 0) {
          LOG.info("{} scheduled for processing", sampleCount);
        }
        futures.put(sample.getAccession(), executorService.submit(task));
      }

      LOG.info("waiting for futures");
      // wait for anything to finish
      ThreadUtils.checkAndCallbackFutures(futures, 0, pipelineFutureCallback);
    } catch (final Exception e) {
      LOG.error("Pipeline failed to finish successfully", e);
      isPassed = false;
      MailSender.sendEmail(
          "Curation",
          "Failed for network connectivity issues/ other issues - <ALERT BIOSAMPLES DEV> "
              + e.getMessage(),
          isPassed);
      throw e;
    } finally {
      Instant endTime = Instant.now();
      LOG.info("Total samples processed {}", sampleCount);
      LOG.info("Total curation objects added {}", pipelineFutureCallback.getTotalCount());
      LOG.info("Pipeline finished at {}", endTime);
      LOG.info(
          "Pipeline total running time {} seconds",
          Duration.between(startTime, endTime).getSeconds());

      PipelineAnalytics pipelineAnalytics =
          new PipelineAnalytics(
              "curation", startTime, endTime, sampleCount, pipelineFutureCallback.getTotalCount());
      pipelineAnalytics.setDateRange(filters);
      analyticsService.persistPipelineAnalytics(pipelineAnalytics);

      // now print a list of things that failed
      final ConcurrentLinkedQueue<String> failedQueue = SampleCurationCallable.failedQueue;

      if (!failedQueue.isEmpty()) {
        // put the first ones on the queue into a list
        // limit the size of list to avoid overload
        List<String> fails = new LinkedList<>();
        while (failedQueue.peek() != null) {
          fails.add(failedQueue.poll());
        }

        final String failures = "Failed files (" + fails.size() + ") " + String.join(" , ", fails);
        LOG.info(failures);
        MailSender.sendEmail("Curation", failures, isPassed);
      }
    }
  }
}
