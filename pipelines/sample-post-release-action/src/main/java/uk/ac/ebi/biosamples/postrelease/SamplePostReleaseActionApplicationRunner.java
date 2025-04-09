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
package uk.ac.ebi.biosamples.postrelease;

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
import uk.ac.ebi.biosamples.PipelinesProperties;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.core.model.Sample;
import uk.ac.ebi.biosamples.core.model.filter.Filter;
import uk.ac.ebi.biosamples.utils.PipelineUtils;
import uk.ac.ebi.biosamples.utils.thread.AdaptiveThreadPoolExecutor;
import uk.ac.ebi.biosamples.utils.thread.ThreadUtils;

@Component
public class SamplePostReleaseActionApplicationRunner implements ApplicationRunner {
  private static final Logger LOG =
      LoggerFactory.getLogger(SamplePostReleaseActionApplicationRunner.class);
  private final BioSamplesClient bioSamplesWebinClient;
  private final PipelinesProperties pipelinesProperties;

  public SamplePostReleaseActionApplicationRunner(
      final BioSamplesClient bioSamplesWebinClient, final PipelinesProperties pipelinesProperties) {
    this.bioSamplesWebinClient = bioSamplesWebinClient;
    this.pipelinesProperties = pipelinesProperties;
  }

  @Override
  public void run(final ApplicationArguments args) throws Exception {
    final Instant startTime = Instant.now();
    final Collection<Filter> filters = PipelineUtils.getDateFilters(args, "release");
    long sampleCount = 0;

    try (final AdaptiveThreadPoolExecutor executorService =
        AdaptiveThreadPoolExecutor.create(
            100,
            10000,
            true,
            pipelinesProperties.getThreadCount(),
            pipelinesProperties.getThreadCountMax())) {

      final Map<String, Future<Boolean>> futures = new HashMap<>();
      for (final EntityModel<Sample> sampleResource :
          bioSamplesWebinClient.fetchSampleResourceAllWithoutCuration("", filters)) {
        final Sample sample = sampleResource.getContent();

        if (sample == null) {
          throw new RuntimeException("Sample should not be null");
        }

        LOG.info("Handling {}", sample.getAccession());

        final Callable<Boolean> task =
            new SamplePostReleaseActionCallable(bioSamplesWebinClient, sample);
        sampleCount++;

        if (sampleCount % 10000 == 0) {
          LOG.info("{} scheduled for processing", sampleCount);
        }

        futures.put(sample.getAccession(), executorService.submit(task));
      }

      LOG.info("waiting for futures");
      // wait for anything to finish
      ThreadUtils.checkFutures(futures, 100);
    } catch (final Exception e) {
      LOG.error("Pipeline failed to finish successfully", e);

      throw e;
    } finally {
      final Instant endTime = Instant.now();
      final String failures;

      LOG.info("Total samples processed {}", sampleCount);
      LOG.info("Pipeline finished at {}", endTime);
      LOG.info(
          "Pipeline total running time {} seconds",
          Duration.between(startTime, endTime).getSeconds());

      // now print a list of things that failed
      final ConcurrentLinkedQueue<String> failedQueue = SamplePostReleaseActionCallable.failedQueue;

      if (!failedQueue.isEmpty()) {
        // put the first ones on the queue into a list
        // limit the size of list to avoid overload
        final List<String> fails = new LinkedList<>();

        while (failedQueue.peek() != null) {
          fails.add(failedQueue.poll());
        }

        failures = "Failed files (" + fails.size() + ") " + String.join(" , ", fails);

        LOG.info(failures);
      }
    }
  }
}
