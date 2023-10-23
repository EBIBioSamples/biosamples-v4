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
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
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
public class NcbiEnaLinkRunner implements ApplicationRunner {
  private static final Logger log = LoggerFactory.getLogger(NcbiEnaLinkRunner.class);
  @Autowired private PipelinesProperties pipelinesProperties;
  @Autowired private EraProDao eraProDao;
  @Autowired private NcbiEnaLinkCallableFactory ncbiEnaLinkCallableFactory;
  @Autowired private MongoPipelineRepository mongoPipelineRepository;

  private final Map<String, Future<Void>> futures = new LinkedHashMap<>();
  private static final Map<String, String> failures = new HashMap<>();

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
      final MongoPipeline mongoPipeline;

      if (isPassed) {
        mongoPipeline =
            new MongoPipeline(
                PipelineUniqueIdentifierGenerator.getPipelineUniqueIdentifier(PipelineName.ENA),
                new Date(),
                PipelineName.NCBIENALINK.name(),
                PipelineCompletionStatus.COMPLETED,
                String.join(",", failures.keySet()),
                null);
      } else {
        mongoPipeline =
            new MongoPipeline(
                PipelineUniqueIdentifierGenerator.getPipelineUniqueIdentifier(PipelineName.ENA),
                new Date(),
                PipelineName.NCBIENALINK.name(),
                PipelineCompletionStatus.FAILED,
                String.join(",", failures.keySet()),
                pipelineFailureCause);
      }

      mongoPipelineRepository.insert(mongoPipeline);

      PipelineUtils.writeFailedSamplesToFile(failures, PipelineName.ENA);
    }
  }

  private void importMissingNcbiSamples(final LocalDate fromDate, final LocalDate toDate) throws Exception {
    log.info("Handling NCBI Samples");

    if (pipelinesProperties.getThreadCount() == 0) {
      final NcbiRowCallbackHandler ncbiRowCallbackHandler =
          new NcbiRowCallbackHandler(null, ncbiEnaLinkCallableFactory, futures);

      eraProDao.getNcbiCallback(fromDate, toDate, ncbiRowCallbackHandler);
    } else {
      try (final AdaptiveThreadPoolExecutor executorService =
          AdaptiveThreadPoolExecutor.create(
              100,
              10000,
              false,
              pipelinesProperties.getThreadCount(),
              pipelinesProperties.getThreadCountMax())) {
        final NcbiRowCallbackHandler ncbiRowCallbackHandler =
            new NcbiRowCallbackHandler(executorService, ncbiEnaLinkCallableFactory, futures);

        eraProDao.getNcbiCallback(fromDate, toDate, ncbiRowCallbackHandler);

        log.info("waiting for futures"); // wait for anything to finish
        ThreadUtils.checkFutures(futures, 0);
      }
    }
  }

  private static class NcbiRowCallbackHandler implements RowCallbackHandler {
    private final AdaptiveThreadPoolExecutor executorService;
    private final NcbiEnaLinkCallableFactory ncbiEnaLinkCallableFactory;
    private final Map<String, Future<Void>> futures;
    private final Logger log = LoggerFactory.getLogger(getClass());

    NcbiRowCallbackHandler(
        final AdaptiveThreadPoolExecutor executorService,
        final NcbiEnaLinkCallableFactory ncbiEnaLinkCallableFactory,
        final Map<String, Future<Void>> futures) {
      this.executorService = executorService;
      this.ncbiEnaLinkCallableFactory = ncbiEnaLinkCallableFactory;
      this.futures = futures;
    }

    @Override
    public void processRow(final ResultSet rs) throws SQLException {
      final String sampleAccession = rs.getString("BIOSAMPLE_ID");
      final java.sql.Date lastUpdated = rs.getDate("LAST_UPDATED");

      log.info(
          String.format(
              "%s is being handled and last updated is %s", sampleAccession, lastUpdated));

      final Callable<Void> callable = ncbiEnaLinkCallableFactory.build(sampleAccession);

      if (executorService == null) {
        try {
          callable.call();
        } catch (final Exception e) {
          throw new RuntimeException(e);
        }
      } else {
        futures.put(sampleAccession, executorService.submit(callable));
        try {
          ThreadUtils.checkFutures(futures, 100);
        } catch (final HttpClientErrorException e) {
          log.error("HTTP Client error body : " + e.getResponseBodyAsString());
          throw e;
        } catch (final ExecutionException e) {
          throw new RuntimeException(e.getCause());
        } catch (final InterruptedException e) {
          throw new RuntimeException(e);
        }
      }
    }
  }
}
