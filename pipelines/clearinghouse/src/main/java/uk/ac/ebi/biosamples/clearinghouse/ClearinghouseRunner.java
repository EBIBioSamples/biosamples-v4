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
package uk.ac.ebi.biosamples.clearinghouse;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.hateoas.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import uk.ac.ebi.biosamples.PipelineFutureCallback;
import uk.ac.ebi.biosamples.PipelineResult;
import uk.ac.ebi.biosamples.PipelinesProperties;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.utils.AdaptiveThreadPoolExecutor;
import uk.ac.ebi.biosamples.utils.MailSender;
import uk.ac.ebi.biosamples.utils.ThreadUtils;

@Component
public class ClearinghouseRunner implements ApplicationRunner {
  private static final Logger LOGGER = LoggerFactory.getLogger(ClearinghouseRunner.class);
  private static final String CLEARINGHOUSE_API_ENDPOINT =
      "https://www.ebi.ac.uk/ena/clearinghouse/api/curations/";
  private final BioSamplesClient bioSamplesClient;
  private final String domain;
  private final PipelineFutureCallback pipelineFutureCallback;

  @Autowired private RestTemplate restTemplate;

  @Autowired private PipelinesProperties pipelinesProperties;

  public ClearinghouseRunner(final BioSamplesClient bioSamplesClient) {
    if (bioSamplesClient.getPublicClient().isPresent()) {
      this.bioSamplesClient = bioSamplesClient.getPublicClient().get();
    } else {
      this.bioSamplesClient = bioSamplesClient;
    }

    this.pipelineFutureCallback = new PipelineFutureCallback();

    // TODO add correct domain for clearinghouse
    domain = "self.bioSamplesClearinghouse";
  }

  @Override
  public void run(ApplicationArguments args) {
    doGetClearingHouseCurations();
  }

  private void doGetClearingHouseCurations() {
    boolean isPassed = true;
    long startTime = System.nanoTime();
    int sampleCount = 0;

    final Map<String, Future<PipelineResult>> futures = new HashMap<>();

    try (final AdaptiveThreadPoolExecutor executorService =
        AdaptiveThreadPoolExecutor.create(
            100,
            10000,
            true,
            pipelinesProperties.getThreadCount(),
            pipelinesProperties.getThreadCountMax())) {
      LOGGER.info("Starting clearinghouse pipeline");

      for (final Resource<Sample> sampleResource : bioSamplesClient.fetchSampleResourceAll()) {
        final Sample sample = sampleResource.getContent();

        try {
          final ResponseEntity<Map> response =
              restTemplate.getForEntity(
                  CLEARINGHOUSE_API_ENDPOINT + sample.getAccession(), Map.class);

          if (response.getStatusCode().is2xxSuccessful()) {
            final Callable<PipelineResult> task =
                new ClearninghouseCallable(
                    bioSamplesClient, sample, domain, (List) response.getBody().get("curations"));

            futures.put(sample.getAccession(), executorService.submit(task));
          }
        } catch (final HttpClientErrorException e) {
          if (e.getStatusCode().value() != 404) {
            LOGGER.error(
                "Failed to retrieve curation from clearinghouse, sample: {}",
                sample.getAccession(),
                e);
          }
        }

        sampleCount++;

        if (sampleCount % 10000 == 0) {
          LOGGER.info("{} scheduled for processing", sampleCount);
        }
      }

      LOGGER.info("waiting for futures");
      ThreadUtils.checkAndCallbackFutures(futures, 0, pipelineFutureCallback);

    } catch (final Exception e) {
      LOGGER.error("Clearinghouse pipeline failed to finish successfully", e);
      isPassed = false;
    } finally {
      long elapsed = System.nanoTime() - startTime;
      final String logMessage =
          "Completed Clearinghouse pipeline:  "
              + sampleCount
              + " samples curated in "
              + (elapsed / 1000000000L)
              + "s";

      LOGGER.info(logMessage);
      MailSender.sendEmail("Clearinghouse pipeline", logMessage, isPassed);
    }
  }
}
