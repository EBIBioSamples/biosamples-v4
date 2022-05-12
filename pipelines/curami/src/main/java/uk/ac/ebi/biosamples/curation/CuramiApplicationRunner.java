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
import uk.ac.ebi.biosamples.model.PipelineAnalytics;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.SampleAnalytics;
import uk.ac.ebi.biosamples.model.filter.Filter;
import uk.ac.ebi.biosamples.mongo.model.MongoCurationRule;
import uk.ac.ebi.biosamples.mongo.repo.MongoCurationRuleRepository;
import uk.ac.ebi.biosamples.utils.AdaptiveThreadPoolExecutor;
import uk.ac.ebi.biosamples.utils.ArgUtils;
import uk.ac.ebi.biosamples.utils.MailSender;
import uk.ac.ebi.biosamples.utils.ThreadUtils;
import uk.ac.ebi.biosamples.utils.mongo.AnalyticsService;

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
      BioSamplesClient bioSamplesClient,
      PipelinesProperties pipelinesProperties,
      MongoCurationRuleRepository repository,
      AnalyticsService analyticsService) {
    this.bioSamplesClient = bioSamplesClient;
    this.pipelinesProperties = pipelinesProperties;
    this.repository = repository;
    this.analyticsService = analyticsService;
    this.curationRules = new HashMap<>();
    this.pipelineFutureCallback = new PipelineFutureCallback();
  }

  @Override
  public void run(ApplicationArguments args) throws Exception {
    Collection<Filter> filters = ArgUtils.getDateFilters(args);
    Instant startTime = Instant.now();
    LOG.info("Pipeline started at {}", startTime);
    long sampleCount = 0;
    boolean isPassed = true;
    SampleAnalytics sampleAnalytics = new SampleAnalytics();

    loadCurationRulesFromFileToDb(getFileNameFromArgs(args));
    curationRules.putAll(loadCurationRulesToMemory());
    LOG.info("Found {} curation rules", curationRules.size());

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

        Callable<PipelineResult> task =
            new SampleCuramiCallable(
                bioSamplesClient, sample, pipelinesProperties.getCurationDomain(), curationRules);
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
      analyticsService.persistSampleAnalytics(startTime, sampleAnalytics);
      analyticsService.persistPipelineAnalytics(pipelineAnalytics);
      MailSender.sendEmail("Curami", handleFailedSamples(), isPassed);
    }
  }

  private Map<String, String> loadCurationRulesToMemory() {
    List<MongoCurationRule> mongoCurationRules = repository.findAll();
    return mongoCurationRules.stream()
        .collect(
            Collectors.toMap(
                MongoCurationRule::getAttributePre, MongoCurationRule::getAttributePost));
  }

  private void loadCurationRulesFromFileToDb(String filePath) {
    Reader reader;
    // read it from given filepath, else read it from classpath
    try {
      if (filePath == null || filePath.isEmpty()) {
        ClassPathResource resource = new ClassPathResource("curation_rules.csv");
        reader = new InputStreamReader(resource.getInputStream());
      } else {
        reader = new FileReader(filePath);
      }
    } catch (IOException e) {
      LOG.error("Could not find specified file in {} or classpath", filePath, e);
      return;
    }

    try (BufferedReader bf = new BufferedReader(reader)) {
      String line = bf.readLine();
      LOG.info("Reading file with headers: {}", line);
      while ((line = bf.readLine()) != null) {
        String[] curationRule = line.split(",");
        MongoCurationRule mongoCurationRule =
            MongoCurationRule.build(curationRule[0].trim(), curationRule[1].trim());
        repository.save(mongoCurationRule);
      }
    } catch (IOException e) {
      LOG.error("Could not find file in {} or classpath", filePath, e);
    }
  }

  private String getFileNameFromArgs(ApplicationArguments args) {
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
