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

import java.io.*;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.hateoas.EntityModel;
import org.springframework.stereotype.Component;
import uk.ac.ebi.biosamples.PipelineFutureCallback;
import uk.ac.ebi.biosamples.PipelineResult;
import uk.ac.ebi.biosamples.PipelinesProperties;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.core.model.PipelineAnalytics;
import uk.ac.ebi.biosamples.core.model.Sample;
import uk.ac.ebi.biosamples.core.model.SampleAnalytics;
import uk.ac.ebi.biosamples.core.model.filter.Filter;
import uk.ac.ebi.biosamples.mongo.model.MongoCurationRule;
import uk.ac.ebi.biosamples.mongo.repository.MongoCurationRuleRepository;
import uk.ac.ebi.biosamples.mongo.service.AnalyticsService;
import uk.ac.ebi.biosamples.utils.PipelineUtils;
import uk.ac.ebi.biosamples.utils.thread.AdaptiveThreadPoolExecutor;
import uk.ac.ebi.biosamples.utils.thread.ThreadUtils;

@Component
public class CuramiApplicationRunner implements ApplicationRunner {
  private static final Logger LOG = LoggerFactory.getLogger(CuramiApplicationRunner.class);

  private final BioSamplesClient bioSamplesClient;
  private final PipelinesProperties pipelinesProperties;
  private final Map<String, String> curationRules;
  private final MongoCurationRuleRepository repository;
  private final AnalyticsService analyticsService;
  private final PipelineFutureCallback pipelineFutureCallback;

  public CuramiApplicationRunner(
      final BioSamplesClient bioSamplesClient,
      final PipelinesProperties pipelinesProperties,
      final MongoCurationRuleRepository repository,
      final AnalyticsService analyticsService) {
    this.bioSamplesClient = bioSamplesClient;
    this.pipelinesProperties = pipelinesProperties;
    this.repository = repository;
    this.analyticsService = analyticsService;
    curationRules = new HashMap<>();
    pipelineFutureCallback = new PipelineFutureCallback();
  }

  @Override
  public void run(final ApplicationArguments args) throws Exception {
    final Collection<Filter> filters = PipelineUtils.getDateFilters(args, "update");
    final Instant startTime = Instant.now();
    LOG.info("Pipeline started at {}", startTime);
    long sampleCount = 0;
    final SampleAnalytics sampleAnalytics = new SampleAnalytics();

    loadCurationRulesFromFileToDb(getFileNameFromArgs(args));
    curationRules.putAll(loadCurationRulesToMemory());
    LOG.info("Found {} curation rules", curationRules.size());

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
        LOG.trace("Handling {}", sampleResource);
        final Sample sample = sampleResource.getContent();
        Objects.requireNonNull(sample);
        collectSampleTypes(sample, sampleAnalytics);

        final Callable<PipelineResult> task =
            new SampleCuramiCallable(
                bioSamplesClient, sample, pipelinesProperties.getProxyWebinId(), curationRules);
        futures.put(sample.getAccession(), executorService.submit(task));

        if (++sampleCount % 5000 == 0) {
          LOG.info("Scheduled sample count {}", sampleCount);
        }
      }

      LOG.info("Waiting for all scheduled tasks to finish");
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
              "curami", startTime, endTime, sampleCount, pipelineFutureCallback.getTotalCount());
      pipelineAnalytics.setDateRange(filters);
      sampleAnalytics.setDateRange(filters);
      sampleAnalytics.setProcessedRecords(sampleCount);
      analyticsService.persistSampleAnalytics(startTime, sampleAnalytics);
      analyticsService.persistPipelineAnalytics(pipelineAnalytics);
    }
  }

  private Map<String, String> loadCurationRulesToMemory() {
    final List<MongoCurationRule> mongoCurationRules = repository.findAll();
    return mongoCurationRules.stream()
        .collect(
            Collectors.toMap(
                MongoCurationRule::getAttributePre, MongoCurationRule::getAttributePost));
  }

  private void loadCurationRulesFromFileToDb(final String filePath) {
    final Reader reader;
    // read it from given filepath, else read it from classpath
    try {
      if (filePath == null || filePath.isEmpty()) {
        final ClassPathResource resource = new ClassPathResource("curation_rules.csv");
        reader = new InputStreamReader(resource.getInputStream());
      } else {
        reader = new FileReader(filePath);
      }
    } catch (final IOException e) {
      LOG.error("Could not find specified file in {} or classpath", filePath, e);
      return;
    }

    try (final BufferedReader bf = new BufferedReader(reader)) {
      String line = bf.readLine();
      LOG.info("Reading file with headers: {}", line);
      while ((line = bf.readLine()) != null) {
        final String[] curationRule = line.split(",");
        final MongoCurationRule mongoCurationRule =
            MongoCurationRule.build(curationRule[0].trim(), curationRule[1].trim());
        repository.save(mongoCurationRule);
      }
    } catch (final IOException e) {
      LOG.error("Could not find file in {} or classpath", filePath, e);
    }
  }

  private String getFileNameFromArgs(final ApplicationArguments args) {
    String curationRulesFile = null;
    if (args.getOptionNames().contains("file")) {
      curationRulesFile = args.getOptionValues("file").get(0);
    }

    return curationRulesFile;
  }

  private String handleFailedSamples() {
    final ConcurrentLinkedQueue<String> failedQueue = SampleCuramiCallable.failedQueue;
    String failures = null;
    if (!failedQueue.isEmpty()) {
      final List<String> fails = new LinkedList<>();
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

  private void collectSampleTypes(final Sample sample, final SampleAnalytics sampleAnalytics) {
    final String accessionPrefix = sample.getAccession().substring(0, 4);
    final String submittedChannel = sample.getSubmittedVia().name();
    sampleAnalytics.addToCenter(accessionPrefix);
    sampleAnalytics.addToChannel(submittedChannel);
  }
}
