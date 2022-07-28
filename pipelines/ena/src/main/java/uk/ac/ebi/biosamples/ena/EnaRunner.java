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
import org.springframework.hateoas.EntityModel;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import uk.ac.ebi.biosamples.PipelinesProperties;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.PipelineName;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.structured.StructuredDataTable;
import uk.ac.ebi.biosamples.mongo.model.MongoPipeline;
import uk.ac.ebi.biosamples.mongo.repo.MongoPipelineRepository;
import uk.ac.ebi.biosamples.mongo.util.PipelineCompletionStatus;
import uk.ac.ebi.biosamples.service.AmrDataLoaderService;
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
public class EnaRunner implements ApplicationRunner {
  private static final Logger log = LoggerFactory.getLogger(EnaRunner.class);
  private static final String SAMEA = "SAMEA";

  private static final int MAX_RETRIES = 5;

  @Autowired private PipelinesProperties pipelinesProperties;
  @Autowired private EraProDao eraProDao;
  @Autowired private EnaCallableFactory enaCallableFactory;
  @Autowired private NcbiCallableFactory ncbiCallableFactory;
  @Autowired private MongoPipelineRepository mongoPipelineRepository;
  @Autowired private AmrDataLoaderService amrDataLoaderService;

  @Autowired
  @Qualifier("WEBINCLIENT")
  private BioSamplesClient bioSamplesWebinClient;

  @Autowired private BioSamplesClient bioSamplesAapClient;
  private Map<String, Future<Void>> futures = new LinkedHashMap<>();
  private Map<String, Set<StructuredDataTable>> sampleToAmrMap = new HashMap<>();

  public static final Map<String, String> failures = new HashMap<>();

  @Override
  public void run(ApplicationArguments args) {
    log.info("Processing ENA pipeline...");

    boolean includeAmr = true;
    boolean processBacklogs = true;
    boolean isPassed = true;

    String pipelineFailureCause = null;

    if (args.getOptionNames().contains("includeAmr")) {
      if (args.getOptionValues("includeAmr").iterator().next().equalsIgnoreCase("false")) {
        includeAmr = false;
      }
    } else {
      includeAmr = false;
    }

    if (args.getOptionNames().contains("processBacklogs")) {
      if (args.getOptionValues("processBacklogs").iterator().next().equalsIgnoreCase("false")) {
        processBacklogs = false;
      }
    } else {
      processBacklogs = false;
    }

    if (includeAmr && isFirstDayOfTheWeek()) {
      try {
        // sampleToAmrMap = amrDataLoaderService.loadAmrData();
      } catch (final Exception e) {
        log.error("Error in processing AMR data from ENA API - continue with the pipeline");
      }
    }

    try {
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
      importEraSamples(fromDate, toDate, sampleToAmrMap);

      if (processBacklogs) {
        backfillEnaBrowserMissingSamples(args, failures);
      }

      /*final List<String> bsdIds = eraProDao.doWWWDEVMapping();

      handleWWWDevMapping(bsdIds);*/
    } catch (final Exception e) {
      log.error("Pipeline failed to finish successfully", e);
      pipelineFailureCause = e.getMessage();
      isPassed = false;
    } finally {
      final MongoPipeline mongoPipeline;

      if (isPassed) {
        mongoPipeline =
            new MongoPipeline(
                PipelineUniqueIdentifierGenerator.getPipelineUniqueIdentifier(PipelineName.ENA),
                new Date(),
                PipelineName.ENA.name(),
                PipelineCompletionStatus.COMPLETED,
                String.join(",", failures.keySet()),
                null);
      } else {
        mongoPipeline =
            new MongoPipeline(
                PipelineUniqueIdentifierGenerator.getPipelineUniqueIdentifier(PipelineName.ENA),
                new Date(),
                PipelineName.ENA.name(),
                PipelineCompletionStatus.FAILED,
                String.join(",", failures.keySet()),
                pipelineFailureCause);
      }

      mongoPipelineRepository.insert(mongoPipeline);

      PipelineUtils.writeFailedSamplesToFile(failures, PipelineName.ENA);
    }
  }

  /*private void handleWWWDevMapping(final List<String> bsdIds) {
    final RestTemplate restTemplate = new RestTemplate();
    final MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();

    headers.add(HttpHeaders.CONTENT_TYPE, MediaTypes.HAL_JSON.toString());

    bsdIds.forEach(
        bsdId -> {
          log.info("Handling BSD ID " + bsdId);

          try {
            final String baseUrl =
                "https://wwwdev.ebi.ac.uk/biosamples/samples/" + bsdId + ".json?curationdomain=";
            final RequestEntity<Void> requestEntity =
                new RequestEntity<>(headers, HttpMethod.GET, URI.create(baseUrl));
            final ResponseEntity<EntityModel<Sample>> responseEntity =
                restTemplate.exchange(
                    requestEntity, new ParameterizedTypeReference<EntityModel<Sample>>() {});

            final EntityModel<Sample> sampleEntityInWWWDev = responseEntity.getBody();

            if (sampleEntityInWWWDev != null) {
              final Sample sampleInWWWDEV = sampleEntityInWWWDev.getContent();

              if (sampleInWWWDEV.getSubmittedVia() == SubmittedViaType.FILE_UPLOADER) {
                final Optional<EntityModel<Sample>> sampleOptionalInProd =
                    bioSamplesWebinClient.fetchSampleResource(bsdId);

                if (sampleOptionalInProd.isPresent()) {
                  final Sample sampleInProd = sampleOptionalInProd.get().getContent();

                  if (sampleInProd.getSubmittedVia() == SubmittedViaType.JSON_API) {
                    log.info(
                        "Sample uploaded using the FILE UPLOADER: "
                            + bsdId
                            + " and reverted in prod while pipeline re-import, merge WWWDEV sample to WWW");

                    final Sample sampleToPostToWWW =
                        Sample.Builder.fromSample(sampleInWWWDEV).build();

                    bioSamplesWebinClient.persistSampleResource(sampleToPostToWWW);
                  } else {
                    log.info("Sample not updated in WWW: " + bsdId);
                  }
                } else {
                  log.info("Sample not found in WWW: " + bsdId);
                }
              } else {
                log.info("Sample not updated in WWWDEV using FILE UPLOADER: " + bsdId);
              }
            } else {
              log.info("Sample not found in WWWDEV: " + bsdId);
            }
          } catch (final Exception e) {
            log.info("Failed to handle BsdId: " + bsdId, e);
          }
        });
  }*/

  private void backfillEnaBrowserMissingSamples(
      final ApplicationArguments args, final Map<String, String> failures)
      throws InterruptedException {
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
                final String errorMessage =
                    "No sample details from ERAPRO for "
                        + sampleId
                        + " possible BioSample Authority sample";
                log.info(errorMessage);

                failures.put(sampleId, errorMessage);
              }
            } catch (Exception e) {
              final String errorMessage =
                  "Failed to handle " + sampleDBBean != null
                      ? sampleDBBean.getBiosampleId()
                      : sampleId + e;
              log.info(errorMessage);

              failures.put(sampleId, errorMessage);
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
            final Optional<EntityModel<Sample>> sampleOptional =
                bioSamplesWebinClient.fetchSampleResource(bioSampleId);

            if (sampleOptional.isPresent()) {
              log.info(
                  "Sample exists, fetch un-curated view and  reset update date " + bioSampleId);

              final Optional<EntityModel<Sample>> optionalSampleResourceWithoutCurations =
                  bioSamplesWebinClient.fetchSampleResource(
                      bioSampleId, Optional.of(curationDomainBlankList));

              final Sample sampleWithoutCurations =
                  optionalSampleResourceWithoutCurations.get().getContent();

              bioSamplesWebinClient.persistSampleResource(
                  Sample.Builder.fromSample(sampleWithoutCurations).build());
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
            final Optional<EntityModel<Sample>> sampleOptional =
                bioSamplesAapClient.fetchSampleResource(bioSampleId);

            if (sampleOptional.isPresent()) {
              log.info("Sample exists, fetch un-curated view and reset update date " + bioSampleId);

              final Optional<EntityModel<Sample>> optionalSampleResourceWithoutCurations =
                  bioSamplesAapClient.fetchSampleResource(
                      bioSampleId, Optional.of(curationDomainBlankList));
              final Sample sampleWithoutCurations =
                  optionalSampleResourceWithoutCurations.get().getContent();

              bioSamplesAapClient.persistSampleResource(
                  Sample.Builder.fromSample(sampleWithoutCurations)
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
