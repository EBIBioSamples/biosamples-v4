/*
* Copyright 2019 EMBL - European Bioinformatics Institute
* Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
* file except in compliance with the License. You may obtain a copy of the License at
* http://www.apache.org/licenses/LICENSE-2.0
* Unless required by applicable law or agreed to in writing, software distributed under the
* License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
* CONDITIONS OF ANY KIND, either express or implied. See the License for the
* specific language governing permissions and limitations under the License.
*/
package uk.ac.ebi.biosamples.curation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.hateoas.Resource;
import org.springframework.stereotype.Component;
import uk.ac.ebi.biosamples.PipelineFutureCallback;
import uk.ac.ebi.biosamples.PipelineResult;
import uk.ac.ebi.biosamples.PipelinesProperties;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.filter.AttributeFilter;
import uk.ac.ebi.biosamples.model.filter.Filter;
import uk.ac.ebi.biosamples.utils.AdaptiveThreadPoolExecutor;
import uk.ac.ebi.biosamples.utils.ArgUtils;
import uk.ac.ebi.biosamples.utils.MailSender;
import uk.ac.ebi.biosamples.utils.ThreadUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;

@Component
public class TransformationApplicationRunner implements ApplicationRunner {
  private static final Logger LOG = LoggerFactory.getLogger(TransformationApplicationRunner.class);

  private final BioSamplesClient bioSamplesClient;
  private final PipelinesProperties pipelinesProperties;
  private final PipelineFutureCallback pipelineFutureCallback;

  public TransformationApplicationRunner(
      BioSamplesClient bioSamplesClient,
      PipelinesProperties pipelinesProperties) {
    this.bioSamplesClient = bioSamplesClient;
    this.pipelinesProperties = pipelinesProperties;
    this.pipelineFutureCallback = new PipelineFutureCallback();
  }

  @Override
  public void run(ApplicationArguments args) throws Exception {
    Collection<Filter> filters = ArgUtils.getDateFilters(args);
    Instant startTime = Instant.now();
    LOG.info("Pipeline started at {}", startTime);
    long sampleCount = 0;
    boolean isPassed = true;

    try (AdaptiveThreadPoolExecutor executorService =
        AdaptiveThreadPoolExecutor.create(
            100,
            10000,
            true,
            pipelinesProperties.getThreadCount(),
            pipelinesProperties.getThreadCountMax())) {

      Map<String, Future<PipelineResult>> futures = new HashMap<>();
      filters.add(new AttributeFilter.Builder("project name").withValue("DTOL").build());
      for (Resource<Sample> sampleResource : bioSamplesClient.fetchSampleResourceAll("", filters)) {
        LOG.trace("Handling {}", sampleResource);
        Sample sample = sampleResource.getContent();
        Objects.requireNonNull(sample);

        Callable<PipelineResult> task =
                new TransformationCallable(bioSamplesClient, sample);
        futures.put(sample.getAccession(), executorService.submit(task));

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
      LOG.info("Total samples modified {}", pipelineFutureCallback.getTotalCount());
      LOG.info("Pipeline finished at {}", endTime);
      LOG.info("Pipeline total running time {} seconds", Duration.between(startTime, endTime).getSeconds());

      MailSender.sendEmail("DTOL relationship transformation", handleFailedSamples(), isPassed);
    }
  }

  private String handleFailedSamples() {
    final ConcurrentLinkedQueue<String> failedQueue = TransformationCallable.failedQueue;
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