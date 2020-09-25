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
import uk.ac.ebi.biosamples.model.structured.AbstractData;
import uk.ac.ebi.biosamples.service.AmrDataLoaderService;
import uk.ac.ebi.biosamples.utils.AdaptiveThreadPoolExecutor;
import uk.ac.ebi.biosamples.utils.MailSender;
import uk.ac.ebi.biosamples.utils.ThreadUtils;

@Component
@ConditionalOnProperty(
    prefix = "job.autorun",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
public class EnaRunner implements ApplicationRunner {
  private static final Logger log = LoggerFactory.getLogger(EnaRunner.class);
  @Autowired private PipelinesProperties pipelinesProperties;
  @Autowired private EraProDao eraProDao;
  @Autowired private EnaCallableFactory enaCallableFactory;
  @Autowired private NcbiCurationCallableFactory ncbiCallableFactory;
  @Autowired private AmrDataLoaderService amrDataLoaderService;

  private Map<String, Future<Void>> futures = new LinkedHashMap<>();
  private Map<String, Set<AbstractData>> sampleToAmrMap = new HashMap<>();

  @Override
  public void run(ApplicationArguments args) {
    boolean isPassed = true;
    boolean includeAmr = true;

    if (args.getOptionNames().contains("includeAmr")) {
      if (args.getOptionValues("includeAmr").iterator().next().equalsIgnoreCase("false")) {
        includeAmr = false;
      }
    }

    if (includeAmr) {
      try {
        sampleToAmrMap = amrDataLoaderService.loadAmrData();
      } catch (final Exception e) {
        log.error("Error in processing AMR data from ENA API - continue with the pipeline");
      }
    }

    try {
      log.info("Processing ENA pipeline...");
      // date format is YYYY-mm-dd
      LocalDate fromDate;
      LocalDate toDate;
      boolean suppressionRunner = true;
      boolean killedRunner = true;

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

      if (args.getOptionNames().contains("suppressionRunner")) {
        if (args.getOptionValues("suppressionRunner").iterator().next().equalsIgnoreCase("false")) {
          suppressionRunner = false;
        }
      }

      if (args.getOptionNames().contains("killedRunner")) {
        if (args.getOptionValues("killedRunner").iterator().next().equalsIgnoreCase("false")) {
          suppressionRunner = false;
        }
      }

      log.info("Suppression Runner is to be executed: " + suppressionRunner);
      log.info("Killed Runner is to be executed: " + killedRunner);

      // Import ENA samples
      importEraSamples(fromDate, toDate, sampleToAmrMap);

      // Handler to append SRA Accession (ENA accession numbers to samples owned by BioSamples)
      importEraBsdAuthoritySamples(fromDate, toDate);

      if (suppressionRunner) {
        // handler for suppressed ENA samples
        handleSuppressedEnaSamples();
        // handler for suppressed NCBI/DDBJ samples
        handleSuppressedNcbiDdbjSamples();
      }

      if (killedRunner) {
        handleKilledEnaSamples();
      }
    } catch (final Exception e) {
      log.error("Pipeline failed to finish successfully", e);
      isPassed = false;
    } finally {
      MailSender.sendEmail("ENA", null, isPassed);
    }
  }

  private void importEraBsdAuthoritySamples(final LocalDate fromDate, final LocalDate toDate)
      throws Exception {
    log.info("Handling BioSamples Authority Samples");

    if (pipelinesProperties.getThreadCount() == 0) {
      final EraRowBsdSamplesCallbackHandler eraRowBsdSamplesCallbackHandler =
          new EraRowBsdSamplesCallbackHandler(null, enaCallableFactory, futures);

      eraProDao.doSampleCallbackBsdAuthoritySamples(
          fromDate, toDate, eraRowBsdSamplesCallbackHandler);
    } else {
      try (final AdaptiveThreadPoolExecutor executorService =
          AdaptiveThreadPoolExecutor.create(
              100,
              10000,
              false,
              pipelinesProperties.getThreadCount(),
              pipelinesProperties.getThreadCountMax())) {
        final EraRowBsdSamplesCallbackHandler eraRowBsdSamplesCallbackHandler =
            new EraRowBsdSamplesCallbackHandler(executorService, enaCallableFactory, futures);

        eraProDao.doSampleCallbackBsdAuthoritySamples(
            fromDate, toDate, eraRowBsdSamplesCallbackHandler);

        log.info("waiting for futures"); // wait for anything to finish
        ThreadUtils.checkFutures(futures, 0);
      }
    }
  }

  private void importEraSamples(
      final LocalDate fromDate,
      final LocalDate toDate,
      final Map<String, Set<AbstractData>> sampleToAmrMap)
      throws Exception {
    log.info("Handling ENA and NCBI Samples");

    if (pipelinesProperties.getThreadCount() == 0) {
      final EraRowCallbackHandler eraRowCallbackHandler =
          new EraRowCallbackHandler(null, enaCallableFactory, futures, sampleToAmrMap);

      eraProDao.doSampleCallback(fromDate, toDate, eraRowCallbackHandler);

      final NcbiRowCallbackHandler ncbiRowCallbackHandler =
          new NcbiRowCallbackHandler(null, ncbiCallableFactory, futures);

      eraProDao.getNcbiCallback(fromDate, toDate, ncbiRowCallbackHandler);
    } else {
      try (final AdaptiveThreadPoolExecutor executorService =
          AdaptiveThreadPoolExecutor.create(
              100,
              10000,
              false,
              pipelinesProperties.getThreadCount(),
              pipelinesProperties.getThreadCountMax())) {

        final EraRowCallbackHandler eraRowCallbackHandler =
            new EraRowCallbackHandler(executorService, enaCallableFactory, futures, sampleToAmrMap);

        eraProDao.doSampleCallback(fromDate, toDate, eraRowCallbackHandler);

        final NcbiRowCallbackHandler ncbiRowCallbackHandler =
            new NcbiRowCallbackHandler(executorService, ncbiCallableFactory, futures);

        eraProDao.getNcbiCallback(fromDate, toDate, ncbiRowCallbackHandler);

        log.info("waiting for futures"); // wait for anything to finish
        ThreadUtils.checkFutures(futures, 0);
      }
    }
  }

  /**
   * Handler for suppressed ENA samples. If status of sample is different in BioSamples, status will
   * be updated so SUPPRESSED. If sample doesn't exist it will be created
   *
   * @throws Exception in case of failures
   */
  private void handleSuppressedEnaSamples() throws Exception {
    log.info(
        "Fetching all suppressed ENA samples. "
            + "If they exist in BioSamples with different status, their status will be updated. If the sample don't exist at all it will be POSTed to BioSamples client");

    try (final AdaptiveThreadPoolExecutor executorService =
        AdaptiveThreadPoolExecutor.create(
            100,
            10000,
            false,
            pipelinesProperties.getThreadCount(),
            pipelinesProperties.getThreadCountMax())) {

      final EnaSuppressedSamplesCallbackHandler enaSuppressedSamplesCallbackHandler =
          new EnaSuppressedSamplesCallbackHandler(executorService, enaCallableFactory, futures);
      eraProDao.doGetSuppressedEnaSamples(enaSuppressedSamplesCallbackHandler);

      log.info("waiting for futures"); // wait for anything to finish
      ThreadUtils.checkFutures(futures, 0);
    }
  }

  /**
   * Handler for killed ENA samples. If status of sample is different in BioSamples, status will be
   * updated so KILLED. If sample doesn't exist it will be created
   *
   * @throws Exception in case of failures
   */
  private void handleKilledEnaSamples() throws Exception {
    log.info(
        "Fetching all killed ENA samples. "
            + "If they exist in BioSamples with different status, their status will be updated. If the sample don't exist at all it will be POSTed to BioSamples client");

    try (final AdaptiveThreadPoolExecutor executorService =
        AdaptiveThreadPoolExecutor.create(
            100,
            10000,
            false,
            pipelinesProperties.getThreadCount(),
            pipelinesProperties.getThreadCountMax())) {

      final EnaKilledSamplesCallbackHandler enaKilledSamplesCallbackHandler =
          new EnaKilledSamplesCallbackHandler(executorService, enaCallableFactory, futures);
      eraProDao.doGetKilledEnaSamples(enaKilledSamplesCallbackHandler);

      log.info("waiting for futures"); // wait for anything to finish
      ThreadUtils.checkFutures(futures, 0);
    }
  }

  /**
   * Handler for suppressed NCBI/DDBJ samples. If status of sample is different in BioSamples,
   * status will be updated to SUPPRESSED
   *
   * @throws Exception in case of failures
   */
  private void handleSuppressedNcbiDdbjSamples() throws Exception {
    log.info(
        "Fetching all suppressed NCBI/DDBJ samples. "
            + "If they exist in BioSamples with different status, their status will be updated. If the sample don't exist at all it will be POSTed to BioSamples client");

    try (final AdaptiveThreadPoolExecutor executorService =
        AdaptiveThreadPoolExecutor.create(
            100,
            10000,
            false,
            pipelinesProperties.getThreadCount(),
            pipelinesProperties.getThreadCountMax())) {

      final NcbiDdbjSuppressedSamplesCallbackHandler ncbiDdbjSuppressedSamplesCallbackHandler =
          new NcbiDdbjSuppressedSamplesCallbackHandler(
              executorService, ncbiCallableFactory, futures);
      eraProDao.doGetSuppressedNcbiDdbjSamples(ncbiDdbjSuppressedSamplesCallbackHandler);

      log.info("waiting for futures"); // wait for anything to finish
      ThreadUtils.checkFutures(futures, 0);
    }
  }

  /**
   * @author dgupta
   *     <p>{@link RowCallbackHandler} for suppressed ENA samples
   */
  private static class EnaSuppressedSamplesCallbackHandler implements RowCallbackHandler {
    private final AdaptiveThreadPoolExecutor executorService;
    private final EnaCallableFactory enaCallableFactory;
    private final Map<String, Future<Void>> futures;

    public EnaSuppressedSamplesCallbackHandler(
        final AdaptiveThreadPoolExecutor executorService,
        final EnaCallableFactory enaCallableFactory,
        final Map<String, Future<Void>> futures) {
      this.executorService = executorService;
      this.enaCallableFactory = enaCallableFactory;
      this.futures = futures;
    }

    @Override
    public void processRow(ResultSet rs) throws SQLException {
      final String sampleAccession = rs.getString("BIOSAMPLE_ID");

      final Callable<Void> callable =
          enaCallableFactory.build(sampleAccession, true, false, false, null);

      if (executorService == null) {
        try {
          callable.call();
        } catch (RuntimeException e) {
          throw e;
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      } else {
        futures.put(sampleAccession, executorService.submit(callable));
      }
    }
  }

  /**
   * @author dgupta
   *     <p>{@link RowCallbackHandler} for killed ENA samples
   */
  private static class EnaKilledSamplesCallbackHandler implements RowCallbackHandler {
    private final AdaptiveThreadPoolExecutor executorService;
    private final EnaCallableFactory enaCallableFactory;
    private final Map<String, Future<Void>> futures;

    public EnaKilledSamplesCallbackHandler(
        final AdaptiveThreadPoolExecutor executorService,
        final EnaCallableFactory enaCallableFactory,
        final Map<String, Future<Void>> futures) {
      this.executorService = executorService;
      this.enaCallableFactory = enaCallableFactory;
      this.futures = futures;
    }

    @Override
    public void processRow(ResultSet rs) throws SQLException {
      final String sampleAccession = rs.getString("BIOSAMPLE_ID");

      final Callable<Void> callable =
          enaCallableFactory.build(sampleAccession, false, true, false, null);

      if (executorService == null) {
        try {
          callable.call();
        } catch (RuntimeException e) {
          throw e;
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      } else {
        futures.put(sampleAccession, executorService.submit(callable));
      }
    }
  }

  /**
   * @author dgupta
   *     <p>{@link RowCallbackHandler} for suppressed NCBI/DDBJ samples
   */
  private static class NcbiDdbjSuppressedSamplesCallbackHandler implements RowCallbackHandler {
    private final AdaptiveThreadPoolExecutor executorService;
    private final NcbiCurationCallableFactory ncbiCurationCallableFactory;
    private final Map<String, Future<Void>> futures;

    public NcbiDdbjSuppressedSamplesCallbackHandler(
        final AdaptiveThreadPoolExecutor executorService,
        final NcbiCurationCallableFactory ncbiCurationCallableFactory,
        final Map<String, Future<Void>> futures) {
      this.executorService = executorService;
      this.ncbiCurationCallableFactory = ncbiCurationCallableFactory;
      this.futures = futures;
    }

    @Override
    public void processRow(ResultSet rs) throws SQLException {
      final String sampleAccession = rs.getString("BIOSAMPLE_ID");

      final Callable<Void> callable = ncbiCurationCallableFactory.build(sampleAccession, true);

      if (executorService == null) {
        try {
          callable.call();
        } catch (RuntimeException e) {
          throw e;
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      } else {
        futures.put(sampleAccession, executorService.submit(callable));
      }
    }
  }

  private static class EraRowCallbackHandler implements RowCallbackHandler {
    private final AdaptiveThreadPoolExecutor executorService;
    private final EnaCallableFactory enaCallableFactory;
    private final Map<String, Future<Void>> futures;
    private final Map<String, Set<AbstractData>> sampleToAmrMap;

    public EraRowCallbackHandler(
        final AdaptiveThreadPoolExecutor executorService,
        final EnaCallableFactory enaCallableFactory,
        final Map<String, Future<Void>> futures,
        final Map<String, Set<AbstractData>> sampleToAmrMap) {
      this.executorService = executorService;
      this.enaCallableFactory = enaCallableFactory;
      this.sampleToAmrMap = sampleToAmrMap;
      this.futures = futures;
    }

    private enum ENAStatus {
      PUBLIC(4),
      SUPPRESSED(5),
      KILLED(6),
      TEMPORARY_SUPPRESSED(7),
      TEMPORARY_KILLED(8);

      private int value;
      private static Map<Integer, ENAStatus> map = new HashMap<>();

      ENAStatus(int value) {
        this.value = value;
      }

      static {
        for (final ENAStatus enaStatus : ENAStatus.values()) {
          map.put(enaStatus.value, enaStatus);
        }
      }

      public static ENAStatus valueOf(int pageType) {
        return map.get(pageType);
      }
    }

    @Override
    public void processRow(ResultSet rs) throws SQLException {
      final String sampleAccession = rs.getString("BIOSAMPLE_ID");
      final int statusID = rs.getInt("STATUS_ID");
      final ENAStatus enaStatus = ENAStatus.valueOf(statusID);
      Set<AbstractData> amrData = new HashSet<>();

      if (sampleToAmrMap.containsKey(sampleAccession)) {
        amrData = sampleToAmrMap.get(sampleAccession);
      }

      switch (enaStatus) {
        case PUBLIC:

        case SUPPRESSED:

        case TEMPORARY_SUPPRESSED:
          log.info(
              String.format(
                  "%s is being handled as status is %s", sampleAccession, enaStatus.name()));
          Callable<Void> callable;
          // update if sample already exists else import

          if (amrData.size() > 0) {
            callable = enaCallableFactory.build(sampleAccession, false, false, false, amrData);
          } else {
            callable = enaCallableFactory.build(sampleAccession, false, false, false, null);
          }
          if (executorService == null) {
            try {
              callable.call();
            } catch (RuntimeException e) {
              throw e;
            } catch (Exception e) {
              throw new RuntimeException(e);
            }
          } else {
            futures.put(sampleAccession, executorService.submit(callable));
            try {
              ThreadUtils.checkFutures(futures, 100);
            } catch (ExecutionException e) {
              throw new RuntimeException(e.getCause());
            } catch (InterruptedException e) {
              throw new RuntimeException(e);
            }
          }

          break;

        case KILLED:

        case TEMPORARY_KILLED:
          log.info(
              String.format(
                  "%s would be handled as status is %s", sampleAccession, enaStatus.name()));
          break;

        default:
          log.info(
              String.format(
                  "%s would be ignored  as status is %s", sampleAccession, enaStatus.name()));
      }
    }
  }

  private static class NcbiRowCallbackHandler implements RowCallbackHandler {
    private final AdaptiveThreadPoolExecutor executorService;
    private final NcbiCurationCallableFactory ncbiCallableFactory;
    private final Map<String, Future<Void>> futures;
    private Logger log = LoggerFactory.getLogger(getClass());

    public NcbiRowCallbackHandler(
        final AdaptiveThreadPoolExecutor executorService,
        final NcbiCurationCallableFactory ncbiCallableFactory,
        final Map<String, Future<Void>> futures) {
      this.executorService = executorService;
      this.ncbiCallableFactory = ncbiCallableFactory;
      this.futures = futures;
    }

    @Override
    public void processRow(ResultSet rs) throws SQLException {
      String sampleAccession = rs.getString("BIOSAMPLE_ID");

      Callable<Void> callable = ncbiCallableFactory.build(sampleAccession);

      if (executorService == null) {
        try {
          callable.call();
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      } else {
        futures.put(sampleAccession, executorService.submit(callable));
        try {
          ThreadUtils.checkFutures(futures, 100);
        } catch (HttpClientErrorException e) {
          log.error("HTTP Client error body : " + e.getResponseBodyAsString());
          throw e;
        } catch (ExecutionException e) {
          throw new RuntimeException(e.getCause());
        } catch (InterruptedException e) {
          throw new RuntimeException(e);
        }
      }
    }
  }

  private static class EraRowBsdSamplesCallbackHandler implements RowCallbackHandler {
    private final AdaptiveThreadPoolExecutor executorService;
    private final EnaCallableFactory enaCallableFactory;
    private final Map<String, Future<Void>> futures;

    public EraRowBsdSamplesCallbackHandler(
        final AdaptiveThreadPoolExecutor executorService,
        final EnaCallableFactory enaCallableFactory,
        final Map<String, Future<Void>> futures) {
      this.executorService = executorService;
      this.enaCallableFactory = enaCallableFactory;
      this.futures = futures;
    }

    @Override
    public void processRow(ResultSet rs) throws SQLException {
      final String sampleAccession = rs.getString("BIOSAMPLE_ID");

      Callable<Void> callable = enaCallableFactory.build(sampleAccession, false, false, true, null);
      if (executorService == null) {
        try {
          callable.call();
        } catch (RuntimeException e) {
          throw e;
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      } else {
        futures.put(sampleAccession, executorService.submit(callable));
      }
    }
  }
}
