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

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import uk.ac.ebi.biosamples.PipelinesProperties;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.Sample;

public class SampleReleaseCallable implements Callable<String> {
  private static final Logger log = LoggerFactory.getLogger(SampleReleaseCallable.class);
  private final PipelinesProperties pipelinesProperties;
  private final BioSamplesClient bioSamplesClient;
  private final RestTemplate restTemplate;
  private final String accession;

  static final ConcurrentLinkedQueue<String> failedQueue = new ConcurrentLinkedQueue<>();

  public SampleReleaseCallable(
      final BioSamplesClient bioSamplesClient,
      final RestTemplate restTemplate,
      PipelinesProperties pipelinesProperties,
      final String accession) {
    this.bioSamplesClient = bioSamplesClient;
    this.restTemplate = restTemplate;
    this.pipelinesProperties = pipelinesProperties;
    this.accession = accession;
  }

  @Override
  public String call() {
    final Map<String, String> params = new HashMap<>();

    try {
      log.info("Handling sample with accession " + accession);
      final Optional<Resource<Sample>> optionalSampleResource =
          bioSamplesClient.fetchSampleResource(accession);

      if (optionalSampleResource.isPresent()) {
        Sample sample = optionalSampleResource.get().getContent();
        bioSamplesClient
            .persistSampleResource(
                Sample.Builder.fromSample(sample).withRelease(Instant.now()).build())
            .getContent();

        params.put("biosampleAccession", accession);

        final ResponseEntity response =
            restTemplate.exchange(
                pipelinesProperties.getWebinEraServiceSampleReleaseDelete(),
                HttpMethod.DELETE,
                new HttpEntity<>(SampleReleaseUtil.createHeaders("era", "password")),
                ResponseEntity.class,
                params);

        final HttpStatus deleteApiStatusCode = response.getStatusCode();
        log.info("Delete response is " + deleteApiStatusCode + " for " + accession);

        if (!deleteApiStatusCode.is2xxSuccessful()) {
          failedQueue.add("Failed to delete " + accession);
        }

        return accession;
      } else {
        failedQueue.add("Failed to find " + accession);
      }
    } catch (final Exception e) {
      failedQueue.add("Exception in processing " + accession);

      return null;
    }

    return null;
  }
}
