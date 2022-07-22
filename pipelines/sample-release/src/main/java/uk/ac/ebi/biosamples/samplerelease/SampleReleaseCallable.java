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
import java.util.*;
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

public class SampleReleaseCallable implements Callable<Void> {
  private static final Logger log = LoggerFactory.getLogger(SampleReleaseCallable.class);
  private final PipelinesProperties pipelinesProperties;
  private final BioSamplesClient bioSamplesWebinClient;
  private final BioSamplesClient bioSamplesAapClient;
  private final RestTemplate restTemplate;
  private final String accession;
  private final List<String> curationDomainBlankList;

  static final ConcurrentLinkedQueue<String> failedQueue = new ConcurrentLinkedQueue<>();

  public SampleReleaseCallable(
      final BioSamplesClient bioSamplesWebinClient,
      final BioSamplesClient bioSamplesAapClient,
      PipelinesProperties pipelinesProperties,
      RestTemplate restTemplate,
      final String accession) {
    this.bioSamplesWebinClient = bioSamplesWebinClient;
    this.bioSamplesAapClient = bioSamplesAapClient;
    this.restTemplate = restTemplate;
    this.pipelinesProperties = pipelinesProperties;
    this.accession = accession;

    curationDomainBlankList = new ArrayList<>();
    curationDomainBlankList.add("");
  }

  @Override
  public Void call() {
    boolean isAap = false;
    boolean isHandled = false;

    try {
      log.info("Handling sample with accession " + accession);

      Optional<Resource<Sample>> optionalSampleResource =
          bioSamplesWebinClient.fetchSampleResource(accession);

      if (!optionalSampleResource.isPresent()) {
        optionalSampleResource = bioSamplesAapClient.fetchSampleResource(accession);
        isAap = true;
      }

      if (optionalSampleResource.isPresent()) {
        final Sample sample = optionalSampleResource.get().getContent();

        log.info("Sample with accession " + sample.getAccession() + " exists in BioSamples");

        if (sample.getRelease().isAfter(Instant.now())) {
          // private sample, make it public
          if (isAap) {
            // re-fetch the sample without curations, passing curation domain list as blank to the
            // client API fails with an exception if the sample is not present
            // hence this workaround of fetching twice
            // TODO: fix the curation domain blank list based find in client
            final Optional<Resource<Sample>> optionalSampleResourceWithoutCurations =
                bioSamplesWebinClient.fetchSampleResource(
                    accession, Optional.of(curationDomainBlankList));

            final Sample sampleWithoutCurations =
                optionalSampleResourceWithoutCurations.get().getContent();

            bioSamplesAapClient
                .persistSampleResource(
                    Sample.Builder.fromSample(sampleWithoutCurations)
                        .withRelease(Instant.now())
                        .build())
                .getContent();
          } else {
            // re-fetch the sample without curations, passing curation domain list as blank to the
            // client API fails with an exception if the sample is not present
            // hence this workaround of fetching twice
            // TODO: fix the curation domain blank list based find in client
            final Optional<Resource<Sample>> optionalSampleResourceWithoutCurations =
                bioSamplesWebinClient.fetchSampleResource(
                    accession, Optional.of(curationDomainBlankList));

            final Sample sampleWithoutCurations =
                optionalSampleResourceWithoutCurations.get().getContent();

            bioSamplesWebinClient
                .persistSampleResource(
                    Sample.Builder.fromSample(sampleWithoutCurations)
                        .withRelease(Instant.now())
                        .build())
                .getContent();
          }
        }

        isHandled = true;
      } else {
        log.info("Failed to find " + accession + " in BioSamples");
        failedQueue.add("Failed to find " + accession + " in BioSamples");
      }

      if (isHandled) {
        final ResponseEntity response = deleteSampleReleaseMessageInEna(accession);

        final HttpStatus deleteApiStatusCode = response.getStatusCode();
        log.info("Delete response is " + deleteApiStatusCode + " for " + accession);

        if (!deleteApiStatusCode.is2xxSuccessful()) {
          failedQueue.add("Failed to delete " + accession);
        }
      }

      return null;
    } catch (final Exception e) {
      failedQueue.add("Exception in processing " + accession);

      return null;
    }
  }

  private ResponseEntity deleteSampleReleaseMessageInEna(final String accession) {
    final Map<String, String> params = new HashMap<>();

    params.put("biosampleAccession", accession);

    return restTemplate.exchange(
        pipelinesProperties.getWebinEraServiceSampleReleaseDelete(),
        HttpMethod.DELETE,
        new HttpEntity<>(SampleReleaseUtil.createHeaders("era", "password")),
        ResponseEntity.class,
        params);
  }
}
