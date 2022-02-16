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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.stream.Collectors;
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
import uk.ac.ebi.biosamples.utils.ThreadUtils;

@Component
public class SampleReleaseRunner implements ApplicationRunner {
  private static final Logger log = LoggerFactory.getLogger(SampleReleaseRunner.class);
  private final BioSamplesClient bioSamplesWebinClient;
  private final BioSamplesClient bioSamplesAapClient;

  @Autowired private RestTemplate restTemplate;
  @Autowired private PipelinesProperties pipelinesProperties;

  public SampleReleaseRunner(
      @Qualifier("WEBINCLIENT") final BioSamplesClient bioSamplesWebinClient,
      @Qualifier("AAPCLIENT") final BioSamplesClient bioSamplesAapClient) {
    this.bioSamplesWebinClient = bioSamplesWebinClient;
    this.bioSamplesAapClient = bioSamplesAapClient;
  }

  @Override
  public void run(ApplicationArguments args) throws Exception {
    doGetSamplesToRelease();
  }

  private void doGetSamplesToRelease() throws Exception {
    final String webinEraServiceSampleReleaseGetUrl =
        pipelinesProperties.getWebinEraServiceSampleReleaseGet();
    log.info(
        "Starting sample release pipeline, Getting from " + webinEraServiceSampleReleaseGetUrl);

    final Map<String, Future<String>> futures = new HashMap<>();

    final ResponseEntity<List<String>> response =
        restTemplate.exchange(
            webinEraServiceSampleReleaseGetUrl,
            HttpMethod.GET,
            new HttpEntity<>(SampleReleaseUtil.createHeaders("era", "password")),
            new ParameterizedTypeReference<List<String>>() {});

    log.info(
        "Response code "
            + response.getStatusCode()
            + " received "
            + response.getBody().size()
            + " samples from webin-era-service to be made public in BioSamples");

    if (response.getStatusCode().is2xxSuccessful()) {
      final ExecutorService executorService = Executors.newSingleThreadExecutor();

      try {
        response
            .getBody()
            .forEach(
                accession -> {
                  final Callable<String> task =
                      new SampleReleaseCallable(
                          bioSamplesWebinClient,
                          bioSamplesAapClient,
                          pipelinesProperties,
                          restTemplate,
                          accession);

                  futures.put(accession, executorService.submit(task));
                });

        log.info("waiting for futures");
        ThreadUtils.checkFutures(futures, 0);

        log.info(
            "Pipeline completed, samples failed are -> \n"
                + SampleReleaseCallable.failedQueue.stream().collect(Collectors.joining("\n")));
      } catch (final Exception e) {
        log.info("Sample release pipeline failed ", e);
      } finally {
        executorService.shutdown();
        executorService.awaitTermination(1, TimeUnit.MINUTES);
      }
    }
  }
}
