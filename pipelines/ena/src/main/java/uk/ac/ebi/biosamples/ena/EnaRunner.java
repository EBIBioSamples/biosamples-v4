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

import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.hateoas.Resource;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import uk.ac.ebi.biosamples.PipelinesProperties;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.structured.StructuredDataTable;
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
  private static final String SAMEA = "SAMEA";

  private static final int MAX_RETRIES = 5;

  @Autowired private PipelinesProperties pipelinesProperties;
  @Autowired private EraProDao eraProDao;
  @Autowired private EnaCallableFactory enaCallableFactory;
  @Autowired private NcbiCallableFactory ncbiCallableFactory;
  @Autowired private AmrDataLoaderService amrDataLoaderService;

  @Autowired
  @Qualifier("WEBINCLIENT")
  private BioSamplesClient bioSamplesWebinClient;

  @Autowired private BioSamplesClient bioSamplesAapClient;
  private Map<String, Future<Void>> futures = new LinkedHashMap<>();
  private Map<String, Set<StructuredDataTable>> sampleToAmrMap = new HashMap<>();

  @Override
  public void run(ApplicationArguments args) {
    final Set<String> failures = new HashSet<>();
    boolean isPassed = true;
    boolean includeAmr = true;

    if (args.getOptionNames().contains("includeAmr")) {
      if (args.getOptionValues("includeAmr").iterator().next().equalsIgnoreCase("false")) {
        includeAmr = false;
      }
    }

    if (includeAmr && isFirstDayOfTheWeek()) {
      try {
        // sampleToAmrMap = amrDataLoaderService.loadAmrData();
      } catch (final Exception e) {
        log.error("Error in processing AMR data from ENA API - continue with the pipeline");
      }
    }

    try {
      log.info("Processing ENA pipeline...");
      // date format is YYYY-mm-dd
      LocalDate fromDate;
      LocalDate toDate;

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

      // Import ENA samples
      // importEraSamples(fromDate, toDate, sampleToAmrMap);

      backfillEnaBrowserMissingSamples(args, failures);
    } catch (final Exception e) {
      log.error("Pipeline failed to finish successfully", e);
      isPassed = false;
    } finally {
      MailSender.sendEmail("ENA", null, isPassed);
    }
  }

  private void backfillEnaBrowserMissingSamples(
      final ApplicationArguments args, final Set<String> failures) throws InterruptedException {
    final ExecutorService executorService = Executors.newSingleThreadExecutor();
    String enaBackFillerFile = null;

    if (args.getOptionNames().contains("ena_failed_file")) {
      enaBackFillerFile = args.getOptionValues("ena_failed_file").get(0);
    }

    assert enaBackFillerFile != null;

    try (Stream<String> stream = Files.lines(Paths.get(enaBackFillerFile))) {
      stream.forEach(
          sampleId -> {
            final SampleDBBean sampleDBBean =
                eraProDao.getSampleDetailsByEnaSampleId(sampleId.trim());

            try {
              if (sampleDBBean != null) {
                handleSingleSampleBackFill(executorService, sampleDBBean);
              } else {
                log.info(
                    "No sample details from ERAPRO for "
                        + sampleId
                        + " possible BioSample Authority sample");

                failures.add(sampleId);
              }
            } catch (Exception e) {
              log.info("Failed to handle " + sampleDBBean.getBiosampleId(), e);

              failures.add(sampleId);
            }
          });
    } catch (final Exception e) {
      e.printStackTrace();
      log.info(e.getMessage());
    } finally {
      executorService.shutdown();
      executorService.awaitTermination(1, TimeUnit.MINUTES);
    }
  }

  private void handleSingleSampleBackFill(
      final ExecutorService executorService, final SampleDBBean sampleDBBean) {
    final String bioSampleAuthority = sampleDBBean.getBiosampleAuthority();
    final String bioSampleId = sampleDBBean.getBiosampleId();
    final List<String> curationDomainBlankList = new ArrayList<>();
    boolean success = false;
    int numRetry = 0;

    curationDomainBlankList.add("");

    log.info("Handling " + bioSampleId + " / " + sampleDBBean.getSampleId());

    if (bioSampleAuthority.equals("N") && bioSampleId != null) {
      if (bioSampleId.startsWith(SAMEA)) {
        while (!success) {
          try {
            final Optional<Resource<Sample>> sampleOptional =
                bioSamplesWebinClient.fetchSampleResource(
                    bioSampleId, Optional.of(curationDomainBlankList));

            if (sampleOptional.isPresent()) {
              log.info("Sample exists, reset update date " + bioSampleId);

              bioSamplesWebinClient.persistSampleResource(
                  Sample.Builder.fromSample(sampleOptional.get().getContent()).build());
            } else {
              log.info("Sample doesn't exists, fetch from ERAPRO " + bioSampleId);

              final Callable<Void> callable =
                  enaCallableFactory.build(sampleDBBean.getBiosampleId(), null, null);

              executorService.submit(callable).get();
            }

            success = true;
          } catch (Exception e) {
            if (++numRetry == MAX_RETRIES) {
              throw new RuntimeException(
                  "Failed to handle the sample with accession " + bioSampleId);
            }

            success = false;
          }
        }
      } else {
        while (!success) {
          try {
            final Optional<Resource<Sample>> sampleOptional =
                bioSamplesAapClient.fetchSampleResource(
                    bioSampleId, Optional.of(curationDomainBlankList));

            if (sampleOptional.isPresent()) {
              log.info("Sample exists, reset update date " + bioSampleId);
              bioSamplesAapClient.persistSampleResource(
                  Sample.Builder.fromSample(sampleOptional.get().getContent())
                      .withDomain(pipelinesProperties.getNcbiDomain())
                      .withNoWebinSubmissionAccountId()
                      .build());
            } else {
              log.info("Sample doesn't exists, fetch from ERAPRO " + bioSampleId);

              final Callable<Void> callable =
                  ncbiCallableFactory.build(sampleDBBean.getBiosampleId());

              executorService.submit(callable).get();
            }

            success = true;
          } catch (Exception e) {
            if (++numRetry == MAX_RETRIES) {
              throw new RuntimeException(
                  "Failed to handle the sample with accession " + bioSampleId);
            }

            success = false;
          }
        }
      }
    }
  }

  private void importEraSamples(
      final LocalDate fromDate,
      final LocalDate toDate,
      final Map<String, Set<StructuredDataTable>> sampleToAmrMap)
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

  private static class EraRowCallbackHandler implements RowCallbackHandler {
    private final AdaptiveThreadPoolExecutor executorService;
    private final EnaCallableFactory enaCallableFactory;
    private final Map<String, Future<Void>> futures;
    private final Map<String, Set<StructuredDataTable>> sampleToAmrMap;

    public EraRowCallbackHandler(
        final AdaptiveThreadPoolExecutor executorService,
        final EnaCallableFactory enaCallableFactory,
        final Map<String, Future<Void>> futures,
        final Map<String, Set<StructuredDataTable>> sampleToAmrMap) {
      this.executorService = executorService;
      this.enaCallableFactory = enaCallableFactory;
      this.sampleToAmrMap = sampleToAmrMap;
      this.futures = futures;
    }

    private enum ENAStatus {
      PRIVATE(2),
      CANCELLED(3),
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
      final String egaId = rs.getString("EGA_ID");
      final java.sql.Date lastUpdated = rs.getDate("LAST_UPDATED");
      final ENAStatus enaStatus = ENAStatus.valueOf(statusID);
      Set<StructuredDataTable> amrData = new HashSet<>();

      if (sampleToAmrMap.containsKey(sampleAccession)) {
        amrData = sampleToAmrMap.get(sampleAccession);
      }

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
                  sampleAccession, enaStatus.name(), lastUpdated));
          Callable<Void> callable;
          // update if sample already exists else import

          if (!amrData.isEmpty()) {
            callable = enaCallableFactory.build(sampleAccession, egaId, amrData);
          } else {
            callable = enaCallableFactory.build(sampleAccession, egaId, null);
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

        default:
          log.info(
              String.format(
                  "%s would be ignored  as status is %s", sampleAccession, enaStatus.name()));
      }
    }
  }

  private static class NcbiRowCallbackHandler implements RowCallbackHandler {
    private final AdaptiveThreadPoolExecutor executorService;
    private final NcbiCallableFactory ncbiCallableFactory;
    private final Map<String, Future<Void>> futures;
    private Logger log = LoggerFactory.getLogger(getClass());

    public NcbiRowCallbackHandler(
        final AdaptiveThreadPoolExecutor executorService,
        final NcbiCallableFactory ncbiCallableFactory,
        final Map<String, Future<Void>> futures) {
      this.executorService = executorService;
      this.ncbiCallableFactory = ncbiCallableFactory;
      this.futures = futures;
    }

    @Override
    public void processRow(ResultSet rs) throws SQLException {
      String sampleAccession = rs.getString("BIOSAMPLE_ID");
      java.sql.Date lastUpdated = rs.getDate("LAST_UPDATED");

      log.info(
          String.format(
              "%s is being handled and last updated is %s", sampleAccession, lastUpdated));

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

  private boolean isFirstDayOfTheWeek() {
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(new Date());

    return calendar.get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY;
  }
}
