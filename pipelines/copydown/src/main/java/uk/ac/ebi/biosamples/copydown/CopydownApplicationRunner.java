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
package uk.ac.ebi.biosamples.copydown;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.hateoas.EntityModel;
import org.springframework.stereotype.Component;
import uk.ac.ebi.biosamples.PipelineFutureCallback;
import uk.ac.ebi.biosamples.PipelineResult;
import uk.ac.ebi.biosamples.PipelinesProperties;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.PipelineAnalytics;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.filter.Filter;
import uk.ac.ebi.biosamples.utils.AdaptiveThreadPoolExecutor;
import uk.ac.ebi.biosamples.utils.PipelineUtils;
import uk.ac.ebi.biosamples.utils.ThreadUtils;
import uk.ac.ebi.biosamples.utils.mongo.AnalyticsService;

@Component
public class CopydownApplicationRunner implements ApplicationRunner {
  private static final Logger LOG = LoggerFactory.getLogger(CopydownApplicationRunner.class);

  private final BioSamplesClient bioSamplesClient;
  private final PipelinesProperties pipelinesProperties;
  private final AnalyticsService analyticsService;
  private final PipelineFutureCallback pipelineFutureCallback;

  public CopydownApplicationRunner(
      final BioSamplesClient bioSamplesClient,
      final PipelinesProperties pipelinesProperties,
      final AnalyticsService analyticsService) {
    this.bioSamplesClient = bioSamplesClient;
    this.pipelinesProperties = pipelinesProperties;
    this.analyticsService = analyticsService;
    pipelineFutureCallback = new PipelineFutureCallback();
  }

  @Override
  public void run(final ApplicationArguments args) throws Exception {
    final Collection<Filter> filters = PipelineUtils.getDateFilters(args, "update");
    final Instant startTime = Instant.now();
    LOG.info("Pipeline started at {}", startTime);
    long sampleCount = 0;

    try (final AdaptiveThreadPoolExecutor executorService =
        AdaptiveThreadPoolExecutor.create(
            100,
            10000,
            true,
            pipelinesProperties.getThreadCount(),
            pipelinesProperties.getThreadCountMax())) {
      final Map<String, Future<PipelineResult>> futures = new HashMap<>();

      for (final EntityModel<Sample> sampleResource :
          bioSamplesClient.fetchSampleResourceAll("", filters)) {
        LOG.trace("Handling " + sampleResource);
        final Sample sample = sampleResource.getContent();
        sampleCount++;

        if (sample == null) {
          throw new RuntimeException("Sample should not be null");
        }

        final Callable<PipelineResult> task =
            new SampleCopydownCallable(
                bioSamplesClient, sample, pipelinesProperties.getCopydownDomain());

        futures.put(sample.getAccession(), executorService.submit(task));
      }

      LOG.info("waiting for futures");
      // wait for anything to finish
      ThreadUtils.checkAndCallbackFutures(futures, 0, pipelineFutureCallback);
    } catch (final Exception e) {
      LOG.error("Pipeline failed to finish successfully", e);
      throw e;
    } finally {
      final Instant endTime = Instant.now();
      LOG.info("Total samples processed {}", sampleCount);
      LOG.info("Total curation objects added {}", pipelineFutureCallback.getTotalCount());
      LOG.info("Pipeline finished at {}", endTime);
      LOG.info(
          "Pipeline total running time {} seconds",
          Duration.between(startTime, endTime).getSeconds());

      final PipelineAnalytics pipelineAnalytics =
          new PipelineAnalytics(
              "copydown", startTime, endTime, sampleCount, pipelineFutureCallback.getTotalCount());
      pipelineAnalytics.setDateRange(filters);
      analyticsService.persistPipelineAnalytics(pipelineAnalytics);

      // now print a list of things that failed
      final ConcurrentLinkedQueue<String> failedQueue = SampleCopydownCallable.failedQueue;

      if (failedQueue.size() > 0) {
        // put the first ones on the queue into a list
        // limit the size of list to avoid overload
        final List<String> fails = new LinkedList<>();

        while (failedQueue.peek() != null) {
          fails.add(failedQueue.poll());
        }

        final String failures = "Failed files (" + fails.size() + ") " + String.join(" , ", fails);

        LOG.info(failures);
      }
    }
    // TODO re-check existing curations
  }
}
