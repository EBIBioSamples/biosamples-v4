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
import org.springframework.web.client.HttpClientErrorException;
import uk.ac.ebi.biosamples.PipelinesProperties;
import uk.ac.ebi.biosamples.misc.RTHandler;
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
public class EnaImportRunner implements ApplicationRunner {
  private static final Logger log = LoggerFactory.getLogger(EnaImportRunner.class);
  @Autowired private PipelinesProperties pipelinesProperties;
  @Autowired private EraProDao eraProDao;
  @Autowired private EnaImportCallableFactory enaImportCallableFactory;
  @Autowired private MongoPipelineRepository mongoPipelineRepository;
  @Autowired private RTHandler rtHandler;

  private final Map<String, Future<Void>> futures = new LinkedHashMap<>();
  static final Set<String> failures = new HashSet<>();
  static final Set<String> todaysSuppressedSamples = new HashSet<>();
  static final Set<String> todaysKilledSamples = new HashSet<>();

  @Override
  public void run(final ApplicationArguments args) throws Exception {
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

      // log.info("Suppression Runner and killed runner is to be executed: " +
      // importSuppressedAndKilled);

      // Import ENA samples
      // importEraSamples(fromDate, toDate);

      // Import BSD authority samples to update SRA accession
      // importEraBsdAuthoritySamples(fromDate, toDate);

      // rtHandler.parseIdentifiersFromFileAndFixAuth();
      rtHandler.parseIdentifiersFromFileAndFixAuth();

      if (importSuppressedAndKilled) {
        try {
          // handler for suppressed and killed ENA samples
          // handleSuppressedAndKilledEnaSamples();
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
      try {
        final MongoPipeline mongoPipeline;
        final String pipelineUniqueIdentifier =
            PipelineUniqueIdentifierGenerator.getPipelineUniqueIdentifier(PipelineName.ENA);

        if (isPassed) {
          mongoPipeline =
              new MongoPipeline(
                  pipelineUniqueIdentifier,
                  new Date(),
                  PipelineName.ENA.name(),
                  PipelineCompletionStatus.COMPLETED,
                  String.join(",", failures),
                  null);
        } else {
          mongoPipeline =
              new MongoPipeline(
                  pipelineUniqueIdentifier,
                  new Date(),
                  PipelineName.ENA.name(),
                  PipelineCompletionStatus.FAILED,
                  String.join(",", failures),
                  pipelineFailureCause);
        }

        mongoPipelineRepository.insert(mongoPipeline);

        PipelineUtils.writeFailedSamplesToFile(failures, PipelineName.ENA);
        PipelineUtils.writeToFile(todaysSuppressedSamples, PipelineName.ENA, "SUPPRESSED");
      } catch (final Exception e) {
        log.info("Error in persisting pipeline status to database " + e.getMessage());
      }
    }
  }

  /**
   * Handler for suppressed ENA samples. If status of sample is different in BioSamples, status will
   * be updated so SUPPRESSED. If sample doesn't exist it will be created
   *
   * @throws Exception in case of failures
   */
  private void handleSuppressedAndKilledEnaSamples() throws Exception {
    log.info(
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
                  executorService, enaImportCallableFactory, futures, SpecialTypes.SUPPRESSED);
      eraProDao.doGetSuppressedEnaSamples(enaSuppressedAndKilledSamplesCallbackHandler);

      log.info("waiting for futures"); // wait for anything to finish

      checkFutures(100);
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

    private final SpecialTypes specialTypes;

    public EnaSuppressedAndKilledSamplesCallbackHandler(
        final AdaptiveThreadPoolExecutor executorService,
        final EnaImportCallableFactory enaCallableFactory,
        final Map<String, Future<Void>> futures,
        final SpecialTypes specialTypes) {
      this.executorService = executorService;
      this.enaCallableFactory = enaCallableFactory;
      this.futures = futures;
      this.specialTypes = specialTypes;
    }

    @Override
    public void processRow(final ResultSet rs) throws SQLException {
      final String sampleAccession = rs.getString("BIOSAMPLE_ID");
      final Callable<Void> callable = enaCallableFactory.build(sampleAccession, null, specialTypes);

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

    final List<SampleCallbackResult> sampleCallbackResults =
        getAllEnaSamplesToHandle(fromDate, toDate);
    final EraRowHandler eraRowHandler = new EraRowHandler(enaImportCallableFactory);

    if (pipelinesProperties.getThreadCount() == 0) {
      sampleCallbackResults.forEach(
          sampleCallbackResult -> eraRowHandler.processRow(sampleCallbackResult, false));
    } else {
      try (final AdaptiveThreadPoolExecutor executorService =
          AdaptiveThreadPoolExecutor.create(
              100,
              10000,
              false,
              pipelinesProperties.getThreadCount(),
              pipelinesProperties.getThreadCountMax())) {

        sampleCallbackResults.forEach(
            sampleCallbackResult ->
                futures.put(
                    sampleCallbackResult.getBiosampleId(),
                    executorService.submit(
                        Objects.requireNonNull(
                            eraRowHandler.processRow(sampleCallbackResult, false)))));

        checkFutures(100);
      }
    }
  }

  private void checkFutures(final int maxSize) {
    try {
      ThreadUtils.checkFutures(futures, maxSize);
    } catch (final HttpClientErrorException e) {
      log.error("HTTP Client error body : " + e.getResponseBodyAsString());
      throw e;
    } catch (final RuntimeException e) {
      throw e;
    } catch (final ExecutionException e) {
      throw new RuntimeException(e.getCause());
    } catch (final InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  private void importEraBsdAuthoritySamples(final LocalDate fromDate, final LocalDate toDate)
      throws Exception {
    log.info("Handling ENA BioSample authority Samples");

    final List<SampleCallbackResult> sampleCallbackResults =
        getAllEnaBsdAuthoritySamplesToHandle(fromDate, toDate);
    final EraRowHandler eraRowHandler = new EraRowHandler(enaImportCallableFactory);

    log.info("Total number of samples to be handled is " + sampleCallbackResults.size());

    if (pipelinesProperties.getThreadCount() == 0) {
      sampleCallbackResults.forEach(
          sampleCallbackResult -> eraRowHandler.processRow(sampleCallbackResult, true));
    } else {
      try (final AdaptiveThreadPoolExecutor executorService =
          AdaptiveThreadPoolExecutor.create(
              100,
              10000,
              false,
              pipelinesProperties.getThreadCount(),
              pipelinesProperties.getThreadCountMax())) {

        sampleCallbackResults.forEach(
            sampleCallbackResult ->
                futures.put(
                    sampleCallbackResult.getBiosampleId(),
                    executorService.submit(
                        Objects.requireNonNull(
                            eraRowHandler.processRow(sampleCallbackResult, true)))));

        checkFutures(100);
      }
    }
  }

  private List<SampleCallbackResult> getAllEnaSamplesToHandle(
      final LocalDate fromDate, final LocalDate toDate) {
    final int MAX_RETRIES = 5;
    List<SampleCallbackResult> sampleCallbackResults = new ArrayList<>();
    boolean success = false;
    int numRetry = 0;

    while (!success) {
      try {
        sampleCallbackResults = eraProDao.doSampleCallback(fromDate, toDate);

        log.info("Total number of samples to be handled is " + sampleCallbackResults.size());

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

  private List<SampleCallbackResult> getAllEnaBsdAuthoritySamplesToHandle(
      final LocalDate fromDate, final LocalDate toDate) {
    final int MAX_RETRIES = 5;
    List<SampleCallbackResult> sampleCallbackResults = new ArrayList<>();
    boolean success = false;
    int numRetry = 0;

    while (!success) {
      try {
        sampleCallbackResults = eraProDao.doSampleCallbackForBsdAuthoritySamples(fromDate, toDate);

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

  private static class EraRowHandler {
    private final EnaImportCallableFactory enaImportCallableFactory;

    EraRowHandler(final EnaImportCallableFactory enaImportCallableFactory) {
      this.enaImportCallableFactory = enaImportCallableFactory;
    }

    private enum ENAStatus {
      PRIVATE(2),
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

    public Callable<Void> processRow(
        final SampleCallbackResult sampleCallbackResult, final boolean bsdAuthority) {
      final String biosampleId = sampleCallbackResult.getBiosampleId();
      final int statusId = sampleCallbackResult.getStatusId();
      final String egaId = sampleCallbackResult.getEgaId();
      final java.sql.Date lastUpdated = sampleCallbackResult.getLastUpdated();
      final ENAStatus enaStatus = ENAStatus.valueOf(statusId);

      switch (enaStatus) {
        case PUBLIC:
        case PRIVATE:
        case SUPPRESSED:
        case TEMPORARY_SUPPRESSED:
        case KILLED:
        case TEMPORARY_KILLED:
        case CANCELLED:
          log.info(
              String.format(
                  "%s is being handled as status is %s and last updated is %s (searched by first public and last updated)",
                  biosampleId, enaStatus.name(), lastUpdated));

          if (bsdAuthority) {
            return enaImportCallableFactory.build(biosampleId, egaId, SpecialTypes.BSD_AUTHORITY);
          } else {
            return enaImportCallableFactory.build(biosampleId, egaId);
          }
        default:
          log.info(
              String.format("%s would be ignored  as status is %s", biosampleId, enaStatus.name()));
      }

      return null;
    }
  }
}
