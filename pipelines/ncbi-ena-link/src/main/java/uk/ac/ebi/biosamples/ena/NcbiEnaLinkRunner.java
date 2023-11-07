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
package uk.ac.ebi.biosamples.ena;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import uk.ac.ebi.biosamples.PipelinesProperties;
import uk.ac.ebi.biosamples.model.PipelineName;
import uk.ac.ebi.biosamples.mongo.model.MongoPipeline;
import uk.ac.ebi.biosamples.mongo.repo.MongoPipelineRepository;
import uk.ac.ebi.biosamples.mongo.util.PipelineCompletionStatus;
import uk.ac.ebi.biosamples.service.EraProDao;
import uk.ac.ebi.biosamples.service.SampleCallbackResult;
import uk.ac.ebi.biosamples.utils.AdaptiveThreadPoolExecutor;
import uk.ac.ebi.biosamples.utils.PipelineUniqueIdentifierGenerator;
import uk.ac.ebi.biosamples.utils.PipelineUtils;
import uk.ac.ebi.biosamples.utils.ThreadUtils;

@Component
@ConditionalOnProperty(
    prefix = "job.autorun",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
public class NcbiEnaLinkRunner implements ApplicationRunner {
  private static final Logger log = LoggerFactory.getLogger(NcbiEnaLinkRunner.class);
  @Autowired private PipelinesProperties pipelinesProperties;
  @Autowired private EraProDao eraProDao;
  @Autowired private NcbiEnaLinkCallableFactory ncbiEnaLinkCallableFactory;
  @Autowired private MongoPipelineRepository mongoPipelineRepository;

  private final Map<String, Future<Void>> futures = new LinkedHashMap<>();
  public static final Map<String, String> failures = new HashMap<>();

  @Override
  public void run(final ApplicationArguments args) throws Exception {
    log.info("Processing ENA pipeline...");

    boolean isPassed = true;
    String pipelineFailureCause = null;

    try {
      // date format is YYYY-mm-dd
      final LocalDate fromDate;
      final LocalDate toDate;

      if (args.getOptionNames().contains("from")) {
        fromDate =
            LocalDate.parse(
                args.getOptionValues("from").iterator().next(), DateTimeFormatter.ISO_LOCAL_DATE);
      } else {
        fromDate = LocalDate.parse("1000-01-01", DateTimeFormatter.ISO_LOCAL_DATE);
      }

      if (args.getOptionNames().contains("until")) {
        toDate =
            LocalDate.parse(
                args.getOptionValues("until").iterator().next(), DateTimeFormatter.ISO_LOCAL_DATE);
      } else {
        toDate = LocalDate.parse("3000-01-01", DateTimeFormatter.ISO_LOCAL_DATE);
      }

      log.info("Running from date range from " + fromDate + " until " + toDate);

      // Import NCBI missing samples from ENA
      importMissingNcbiSamples(fromDate, toDate);
    } catch (final Exception e) {
      log.error("Pipeline failed to finish successfully", e);
      pipelineFailureCause = e.getMessage();
      isPassed = false;

      throw e;
    } finally {
      try {
        final MongoPipeline mongoPipeline;
        final String pipelineUniqueIdentifier = PipelineUniqueIdentifierGenerator.getPipelineUniqueIdentifier(PipelineName.NCBIENALINK);

        if (isPassed) {
          mongoPipeline =
                  new MongoPipeline(
                          pipelineUniqueIdentifier,
                          new Date(),
                          PipelineName.NCBIENALINK.name(),
                          PipelineCompletionStatus.COMPLETED,
                          String.join(",", failures.keySet()),
                          null);
        } else {
          mongoPipeline =
                  new MongoPipeline(
                          pipelineUniqueIdentifier,
                          new Date(),
                          PipelineName.NCBIENALINK.name(),
                          PipelineCompletionStatus.FAILED,
                          String.join(",", failures.keySet()),
                          pipelineFailureCause);
        }

        mongoPipelineRepository.insert(mongoPipeline);

        PipelineUtils.writeFailedSamplesToFile(failures, PipelineName.ENA);
      }  catch (final Exception e) {
        log.info("Error in persisting pipeline status to database " + e.getMessage());
      }
    }
  }

  private List<SampleCallbackResult> getAllNcbiSamplesToHandle(
          final LocalDate fromDate, final LocalDate toDate) {
    final int MAX_RETRIES = 5;
    List<SampleCallbackResult> sampleCallbackResults = new ArrayList<>();
    boolean success = false;
    int numRetry = 0;

    while (!success) {
      try {
        sampleCallbackResults = eraProDao.doNcbiCallback(fromDate, toDate);

        success = true;
      } catch (final Exception e) {
        log.error("Fetching from ERAPRO failed with exception - retry ", e);

        if (++numRetry == MAX_RETRIES) {
          throw new RuntimeException("Permanent failure in fetching samples from ERAPRO");
        }
      }
    }

    return sampleCallbackResults;
  }

  private void importMissingNcbiSamples(final LocalDate fromDate, final LocalDate toDate) throws Exception {
    log.info("Handling NCBI Samples");

    final List<SampleCallbackResult> sampleCallbackResults =
            getAllNcbiSamplesToHandle(fromDate, toDate);

    if (pipelinesProperties.getThreadCount() == 0) {
      final NcbiRowHandler ncbiRowHandler =
          new NcbiRowHandler(ncbiEnaLinkCallableFactory);

      sampleCallbackResults.forEach(ncbiRowHandler::processRow);
    } else {
      try (final AdaptiveThreadPoolExecutor executorService =
          AdaptiveThreadPoolExecutor.create(
              100,
              10000,
              false,
              pipelinesProperties.getThreadCount(),
              pipelinesProperties.getThreadCountMax())) {
        final NcbiRowHandler ncbiRowHandler =
            new NcbiRowHandler(ncbiEnaLinkCallableFactory);

        sampleCallbackResults.forEach(
                sampleCallbackResult -> {
                  futures.put(
                          sampleCallbackResult.getBiosampleId(),
                          executorService.submit(
                                  Objects.requireNonNull(ncbiRowHandler.processRow(sampleCallbackResult))));
                });

        try {
          ThreadUtils.checkFutures(futures, 100);
        } catch (final ExecutionException e) {
          throw new RuntimeException(e.getCause());
        } catch (final InterruptedException e) {
          throw new RuntimeException(e);
        }

        log.info("waiting for futures"); // wait for anything to finish
        ThreadUtils.checkFutures(futures, 0);
      }
    }
  }

  private static class NcbiRowHandler {
    private final NcbiEnaLinkCallableFactory ncbiEnaLinkCallableFactory;
    private final Logger log = LoggerFactory.getLogger(getClass());

    NcbiRowHandler(
        final NcbiEnaLinkCallableFactory ncbiEnaLinkCallableFactory) {
      this.ncbiEnaLinkCallableFactory = ncbiEnaLinkCallableFactory;
    }

    public Callable<Void> processRow(final SampleCallbackResult sampleCallbackResult) {
      final String sampleAccession = sampleCallbackResult.getBiosampleId();
      final java.sql.Date lastUpdated = sampleCallbackResult.getLastUpdated();

      log.info(
          String.format(
              "%s is being handled and last updated is %s", sampleAccession, lastUpdated));

      return ncbiEnaLinkCallableFactory.build(sampleAccession);
    }
  }
}
