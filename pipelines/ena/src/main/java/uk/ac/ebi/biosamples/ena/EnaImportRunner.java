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

import java.sql.ResultSet;
import java.sql.SQLException;
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
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Component;
import uk.ac.ebi.biosamples.PipelinesProperties;
import uk.ac.ebi.biosamples.model.PipelineName;
import uk.ac.ebi.biosamples.mongo.model.MongoPipeline;
import uk.ac.ebi.biosamples.mongo.repo.MongoPipelineRepository;
import uk.ac.ebi.biosamples.mongo.util.PipelineCompletionStatus;
import uk.ac.ebi.biosamples.service.EraProDao;
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
public class EnaImportRunner implements ApplicationRunner {
  private static final Logger log = LoggerFactory.getLogger(EnaImportRunner.class);
  @Autowired private PipelinesProperties pipelinesProperties;
  @Autowired private EraProDao eraProDao;
  @Autowired private EnaImportCallableFactory enaImportCallableFactory;
  @Autowired private MongoPipelineRepository mongoPipelineRepository;

  private final Map<String, Future<Void>> futures = new LinkedHashMap<>();
  static final Set<String> failures = new HashSet<>();
  static final Set<String> todaysSuppressedSamples = new HashSet<>();
  static final Set<String> todaysKilledSamples = new HashSet<>();

  @Override
  public void run(final ApplicationArguments args) {
    log.info("Processing ENA pipeline...");

    boolean isPassed = true;
    boolean importSuppressedAndKilled = true;

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

      if (args.getOptionNames().contains("importSuppressedAndKilled")) {
        if (args.getOptionValues("importSuppressedAndKilled")
            .iterator()
            .next()
            .equalsIgnoreCase("false")) {
          importSuppressedAndKilled = false;
        }
      }

      log.info(
          "Suppression Runner and killed runner is to be executed: " + importSuppressedAndKilled);

      // Import ENA samples
      // importEraSamples(fromDate, toDate);

      if (importSuppressedAndKilled) {
        try {
          // handler for suppressed and killed ENA samples
          handleSuppressedAndKilledEnaSamples();
        } catch (final Exception e) {
          log.info("Suppression Runner failed");
        }
      }
    } catch (final Exception e) {
      log.error("Pipeline failed to finish successfully", e);
      pipelineFailureCause = e.getMessage();
      isPassed = false;

      throw e;
    } finally {
      final MongoPipeline mongoPipeline;

      if (isPassed) {
        mongoPipeline =
            new MongoPipeline(
                PipelineUniqueIdentifierGenerator.getPipelineUniqueIdentifier(PipelineName.ENA),
                new Date(),
                PipelineName.ENA.name(),
                PipelineCompletionStatus.COMPLETED,
                String.join(",", failures),
                null);
      } else {
        mongoPipeline =
            new MongoPipeline(
                PipelineUniqueIdentifierGenerator.getPipelineUniqueIdentifier(PipelineName.ENA),
                new Date(),
                PipelineName.ENA.name(),
                PipelineCompletionStatus.FAILED,
                String.join(",", failures),
                pipelineFailureCause);
      }

      mongoPipelineRepository.insert(mongoPipeline);

      PipelineUtils.writeFailedSamplesToFile(failures, PipelineName.ENA);
      PipelineUtils.writeToFile(todaysSuppressedSamples, PipelineName.ENA, "SUPPRESSED");
      // PipelineUtils.writeToFile(todaysKilledSamples, PipelineName.ENA, "KILLED");
    }
  }

  /**
   * Handler for suppressed ENA samples. If status of sample is different in BioSamples, status will
   * be updated so SUPPRESSED. If sample doesn't exist it will be created
   *
   * @throws Exception in case of failures
   */
  private void handleSuppressedAndKilledEnaSamples() throws Exception {
    /*log.info(
        "Fetching all suppressed ENA samples. "
            + "If they exist in BioSamples with different status, their status will be updated. ");
    try (final AdaptiveThreadPoolExecutor executorService =
        AdaptiveThreadPoolExecutor.create(
            100,
            10000,
            false,
            pipelinesProperties.getThreadCount(),
            pipelinesProperties.getThreadCountMax())) {

      final EnaSuppressedAndKilledSamplesCallbackHandler
          enaSuppressedAndKilledSamplesCallbackHandler =
              new EnaSuppressedAndKilledSamplesCallbackHandler(
                  executorService,
                  enaImportCallableFactory,
                  futures,
                  SuppressedKilledType.SUPPRESSED);
      eraProDao.doGetSuppressedEnaSamples(enaSuppressedAndKilledSamplesCallbackHandler);

      log.info("waiting for futures"); // wait for anything to finish
      ThreadUtils.checkFutures(futures, 0);
    }

    log.info(
            "Fetching all killed ENA samples. "
                    + "If they exist in BioSamples with different status, their status will be updated. ");*/

    try (final AdaptiveThreadPoolExecutor executorService =
        AdaptiveThreadPoolExecutor.create(
            100,
            10000,
            false,
            pipelinesProperties.getThreadCount(),
            pipelinesProperties.getThreadCountMax())) {

      final EnaSuppressedAndKilledSamplesCallbackHandler
          enaSuppressedAndKilledSamplesCallbackHandler =
              new EnaSuppressedAndKilledSamplesCallbackHandler(
                  executorService, enaImportCallableFactory, futures, SuppressedKilledType.KILLED);
      eraProDao.doGetKilledEnaSamples(enaSuppressedAndKilledSamplesCallbackHandler);

      log.info("waiting for futures"); // wait for anything to finish
      ThreadUtils.checkFutures(futures, 0);
    }
  }

  /**
   * @author dgupta
   *     <p>{@link RowCallbackHandler} for suppressed ENA samples
   */
  private static class EnaSuppressedAndKilledSamplesCallbackHandler implements RowCallbackHandler {
    private final AdaptiveThreadPoolExecutor executorService;
    private final EnaImportCallableFactory enaCallableFactory;
    private final Map<String, Future<Void>> futures;

    private final SuppressedKilledType suppressedKilledType;

    public EnaSuppressedAndKilledSamplesCallbackHandler(
        final AdaptiveThreadPoolExecutor executorService,
        final EnaImportCallableFactory enaCallableFactory,
        final Map<String, Future<Void>> futures,
        final SuppressedKilledType suppressedKilledType) {
      this.executorService = executorService;
      this.enaCallableFactory = enaCallableFactory;
      this.futures = futures;
      this.suppressedKilledType = suppressedKilledType;
    }

    @Override
    public void processRow(final ResultSet rs) throws SQLException {
      final String sampleAccession = rs.getString("BIOSAMPLE_ID");
      final Callable<Void> callable =
          enaCallableFactory.build(sampleAccession, null, suppressedKilledType);

      execute(sampleAccession, callable, executorService, futures);
    }

    private static void execute(
        final String sampleAccession,
        final Callable<Void> callable,
        final AdaptiveThreadPoolExecutor executorService,
        final Map<String, Future<Void>> futures) {
      if (executorService == null) {
        try {
          callable.call();
        } catch (final RuntimeException e) {
          throw e;
        } catch (final Exception e) {
          throw new RuntimeException(e);
        }
      } else {
        futures.put(sampleAccession, executorService.submit(callable));
      }
    }
  }

  private void importEraSamples(final LocalDate fromDate, final LocalDate toDate) throws Exception {
    log.info("Handling ENA Samples");

    if (pipelinesProperties.getThreadCount() == 0) {
      final EraRowCallbackHandler eraRowCallbackHandler =
          new EraRowCallbackHandler(null, enaImportCallableFactory, futures);

      transformEnaSamplesToBioSamplesAndSave(fromDate, toDate, eraRowCallbackHandler);
    } else {
      try (final AdaptiveThreadPoolExecutor executorService =
          AdaptiveThreadPoolExecutor.create(
              100,
              10000,
              false,
              pipelinesProperties.getThreadCount(),
              pipelinesProperties.getThreadCountMax())) {

        final EraRowCallbackHandler eraRowCallbackHandler =
            new EraRowCallbackHandler(executorService, enaImportCallableFactory, futures);

        transformEnaSamplesToBioSamplesAndSave(fromDate, toDate, eraRowCallbackHandler);
        log.info("waiting for futures"); // wait for anything to finish
        ThreadUtils.checkFutures(futures, 0);
      }
    }
  }

  private void transformEnaSamplesToBioSamplesAndSave(
      final LocalDate fromDate,
      final LocalDate toDate,
      final EraRowCallbackHandler eraRowCallbackHandler) {
    final int MAX_RETRIES = 5;
    boolean success = false;
    int numRetry = 0;

    while (!success) {
      try {
        eraProDao.doSampleCallback(fromDate, toDate, eraRowCallbackHandler);

        success = true;
      } catch (final Exception e) {
        log.error("Fetching from ERAPRO failed with exception - retry ", e);

        if (++numRetry == MAX_RETRIES) {
          throw new RuntimeException("Permanent failure in fetching samples from ERAPRO");
        }
      }
    }
  }

  private static class EraRowCallbackHandler implements RowCallbackHandler {
    private final AdaptiveThreadPoolExecutor executorService;
    private final EnaImportCallableFactory enaImportCallableFactory;
    private final Map<String, Future<Void>> futures;

    EraRowCallbackHandler(
        final AdaptiveThreadPoolExecutor executorService,
        final EnaImportCallableFactory enaImportCallableFactory,
        final Map<String, Future<Void>> futures) {
      this.executorService = executorService;
      this.enaImportCallableFactory = enaImportCallableFactory;
      this.futures = futures;
    }

    private enum ENAStatus {
      CANCELLED(3),
      PUBLIC(4),
      SUPPRESSED(5),
      KILLED(6),
      TEMPORARY_SUPPRESSED(7),
      TEMPORARY_KILLED(8);

      private final int value;
      private static final Map<Integer, ENAStatus> enaSampleStatusIdToNameMap = new HashMap<>();

      ENAStatus(final int value) {
        this.value = value;
      }

      static {
        for (final ENAStatus enaStatus : ENAStatus.values()) {
          enaSampleStatusIdToNameMap.put(enaStatus.value, enaStatus);
        }
      }

      public static ENAStatus valueOf(final int pageType) {
        return enaSampleStatusIdToNameMap.get(pageType);
      }
    }

    @Override
    public void processRow(final ResultSet rs) throws SQLException {
      final String biosampleId = rs.getString("BIOSAMPLE_ID");
      final int statusId = rs.getInt("STATUS_ID");
      final String egaId = rs.getString("EGA_ID");
      final java.sql.Date lastUpdated = rs.getDate("LAST_UPDATED");
      final ENAStatus enaStatus = ENAStatus.valueOf(statusId);

      switch (enaStatus) {
        case PUBLIC:
        case SUPPRESSED:
        case TEMPORARY_SUPPRESSED:
        case KILLED:
        case TEMPORARY_KILLED:
        case CANCELLED:
          log.info(
              String.format(
                  "%s is being handled as status is %s and last updated is %s",
                  biosampleId, enaStatus.name(), lastUpdated));
          // update if sample already exists else import
          final Callable<Void> callable = enaImportCallableFactory.build(biosampleId, egaId);

          if (executorService == null) {
            try {
              callable.call();
            } catch (final RuntimeException e) {
              throw e;
            } catch (final Exception e) {
              throw new RuntimeException(e);
            }
          } else {
            futures.put(biosampleId, executorService.submit(callable));

            try {
              ThreadUtils.checkFutures(futures, 100);
            } catch (final ExecutionException e) {
              throw new RuntimeException(e.getCause());
            } catch (final InterruptedException e) {
              throw new RuntimeException(e);
            }
          }

          break;

        default:
          log.info(
              String.format("%s would be ignored  as status is %s", biosampleId, enaStatus.name()));
      }
    }
  }
}
