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
package uk.ac.ebi.biosamples.neoexport;

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
import uk.ac.ebi.biosamples.model.SampleAnalytics;
import uk.ac.ebi.biosamples.model.filter.Filter;
import uk.ac.ebi.biosamples.neo4j.repo.NeoSampleRepository;
import uk.ac.ebi.biosamples.utils.AdaptiveThreadPoolExecutor;
import uk.ac.ebi.biosamples.utils.PipelineUtils;
import uk.ac.ebi.biosamples.utils.ThreadUtils;

@Component
public class NeoExportRunner implements ApplicationRunner {
  private static final Logger LOG = LoggerFactory.getLogger(NeoExportRunner.class);

  private final BioSamplesClient bioSamplesClient;
  private final PipelinesProperties pipelinesProperties;
  private final NeoSampleRepository neoSampleRepository;
  private final PipelineFutureCallback pipelineFutureCallback;
  private final NeoCsvExporter neoCsvExporter;

  public NeoExportRunner(
      BioSamplesClient bioSamplesClient,
      PipelinesProperties pipelinesProperties,
      NeoSampleRepository neoSampleRepository,
      NeoCsvExporter neoCsvExporter) {
    this.bioSamplesClient = bioSamplesClient;
    this.pipelinesProperties = pipelinesProperties;
    this.neoSampleRepository = neoSampleRepository;
    this.neoCsvExporter = neoCsvExporter;
    this.pipelineFutureCallback = new PipelineFutureCallback();
  }

  @Override
  public void run(ApplicationArguments args) throws Exception {
    Collection<Filter> filters = PipelineUtils.getDateFilters(args);
    //    RelationFilter relationFilter = new RelationFilter.Builder("has member").build();
    //    filters.add(relationFilter);
    //    ExternalReferenceDataFilter externalFilter = new ExternalReferenceDataFilter.Builder("EGA
    // Dataset").build();
    //    filters.add(externalFilter);

    Instant startTime = Instant.now();
    LOG.info("Pipeline started at {}", startTime);
    long sampleCount = 0;
    boolean isPassed = true;
    SampleAnalytics sampleAnalytics = new SampleAnalytics();

    String format = "";
    if (args.getOptionNames().contains("format")) {
      format = args.getOptionValues("format").iterator().next();
    }
    if ("CSV".equalsIgnoreCase(format)) {
      LOG.info("Saving into CSV format for later consumption");
    } else {
      LOG.info("Directly exporting to neo4j instance");
    }

    try (AdaptiveThreadPoolExecutor executorService =
        AdaptiveThreadPoolExecutor.create(
            100,
            10000,
            true,
            pipelinesProperties.getThreadCount(),
            pipelinesProperties.getThreadCountMax())) {

      Map<String, Future<PipelineResult>> futures = new HashMap<>();
      for (EntityModel<Sample> sampleResource :
          bioSamplesClient.fetchSampleResourceAll("", filters)) {
        LOG.trace("Handling {}", sampleResource);
        Sample sample = sampleResource.getContent();
        Objects.requireNonNull(sample);
        collectSampleTypes(sample, sampleAnalytics);

        // we will export only relationship containing entities
        if (!sample.getRelationships().isEmpty() || !sample.getExternalReferences().isEmpty()) {
          if ("CSV".equalsIgnoreCase(format)) {
            neoCsvExporter.addToCSVFile(sample);
          } else {
            Callable<PipelineResult> task = new NeoExportCallable(neoSampleRepository, sample);
            futures.put(sample.getAccession(), executorService.submit(task));
          }
        }

        if (++sampleCount % 5000 == 0) {
          LOG.info("Scheduled sample count {}", sampleCount);
        }
      }

      if ("CSV".equalsIgnoreCase(format)) {
        neoCsvExporter.flush();
      } else {
        LOG.info("Waiting for all scheduled tasks to finish");
        ThreadUtils.checkAndCallbackFutures(futures, 0, pipelineFutureCallback);
      }

    } catch (Exception e) {
      LOG.error("Pipeline failed to finish successfully", e);
      isPassed = false;
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
              "curami", startTime, endTime, sampleCount, pipelineFutureCallback.getTotalCount());
      pipelineAnalytics.setDateRange(filters);
      sampleAnalytics.setDateRange(filters);
      sampleAnalytics.setProcessedRecords(sampleCount);
    }
  }

  private String handleFailedSamples() {
    final ConcurrentLinkedQueue<String> failedQueue = NeoExportCallable.failedQueue;
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

  private void collectSampleTypes(Sample sample, SampleAnalytics sampleAnalytics) {
    String accessionPrefix = sample.getAccession().substring(0, 4);
    String submittedChannel = sample.getSubmittedVia().name();
    sampleAnalytics.addToCenter(accessionPrefix);
    sampleAnalytics.addToChannel(submittedChannel);
  }
}
