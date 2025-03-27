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
package uk.ac.ebi.biosamples.samplerelease;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import uk.ac.ebi.biosamples.PipelinesProperties;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.utils.thread.ThreadUtils;

@Component
public class SampleReleaseRunner implements ApplicationRunner {
  private static final Logger log = LoggerFactory.getLogger(SampleReleaseRunner.class);
  private final BioSamplesClient bioSamplesWebinClient;

  @Autowired private RestTemplate restTemplate;
  @Autowired private PipelinesProperties pipelinesProperties;

  public SampleReleaseRunner(
      @Qualifier("WEBINCLIENT") final BioSamplesClient bioSamplesWebinClient) {
    this.bioSamplesWebinClient = bioSamplesWebinClient;
  }

  @Override
  public void run(final ApplicationArguments args) throws Exception {
    // date format is YYYY-mm-dd
    final LocalDate fromDate;
    final LocalDate toDate;
    final Integer numRows;

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

    if (args.getOptionNames().contains("numRows")) {
      numRows = Integer.valueOf(args.getOptionValues("numRows").iterator().next());
    } else {
      numRows = null;
    }

    log.info(
        "Running from date range from "
            + fromDate
            + " until "
            + toDate
            + " and getting "
            + numRows
            + " rows");

    releaseSamples(fromDate, toDate, numRows);
  }

  private void releaseSamples(
      final LocalDate fromDate, final LocalDate toDate, final Integer numRows) throws Exception {
    String webinEraServiceSampleReleaseGetUrl =
        pipelinesProperties.getWebinEraServiceSampleReleaseGet();
    webinEraServiceSampleReleaseGetUrl =
        webinEraServiceSampleReleaseGetUrl
            + "?fromDate="
            + fromDate
            + "&toDate="
            + toDate
            + "&numRows="
            + numRows;

    log.info(
        "Starting sample release pipeline, Getting from " + webinEraServiceSampleReleaseGetUrl);

    final Map<String, Future<Void>> futures = new HashMap<>();

    final ResponseEntity<List<String>> response =
        restTemplate.exchange(
            webinEraServiceSampleReleaseGetUrl,
            HttpMethod.GET,
            new HttpEntity<>(SampleReleaseUtil.createHeaders()),
            new ParameterizedTypeReference<>() {});

    log.info(
        "Response code "
            + response.getStatusCode()
            + " received "
            + Objects.requireNonNull(response.getBody()).size()
            + " samples from webin-era-service to be made public in BioSamples");

    if (response.getStatusCode().is2xxSuccessful()) {
      final ExecutorService executorService = Executors.newSingleThreadExecutor();

      try {
        response
            .getBody()
            .forEach(
                accession -> {
                  final Callable<Void> task =
                      new SampleReleaseCallable(
                          bioSamplesWebinClient,
                          pipelinesProperties,
                          restTemplate,
                          accession,
                          fromDate);

                  futures.put(accession, executorService.submit(task));
                });

        log.info("waiting for futures");

        ThreadUtils.checkFutures(futures, 100);

        log.info(
            "Pipeline completed, samples failed are -> \n"
                + String.join("\n", SampleReleaseCallable.failedQueue));
      } catch (final Exception e) {
        log.info("Sample release pipeline failed ", e);
      } finally {
        executorService.shutdown();
        executorService.awaitTermination(1, TimeUnit.MINUTES);
      }
    }
  }
}
