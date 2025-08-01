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
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import uk.ac.ebi.biosamples.PipelinesProperties;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.core.model.Attribute;
import uk.ac.ebi.biosamples.core.model.ExternalReference;
import uk.ac.ebi.biosamples.core.model.Sample;
import uk.ac.ebi.biosamples.core.model.SampleStatus;

public class SampleReleaseCallable implements Callable<Void> {
  private static final Logger log = LoggerFactory.getLogger(SampleReleaseCallable.class);
  private final PipelinesProperties pipelinesProperties;
  private final BioSamplesClient bioSamplesWebinClient;
  private final RestTemplate restTemplate;
  private final String accession;
  static final ConcurrentLinkedQueue<String> failedQueue = new ConcurrentLinkedQueue<>();

  SampleReleaseCallable(
      final BioSamplesClient bioSamplesWebinClient,
      final PipelinesProperties pipelinesProperties,
      final RestTemplate restTemplate,
      final String accession) {
    this.bioSamplesWebinClient = bioSamplesWebinClient;
    this.restTemplate = restTemplate;
    this.pipelinesProperties = pipelinesProperties;
    this.accession = accession;
  }

  @Override
  public Void call() {
    boolean isHandled = false;

    try {
      log.info("Handling sample with accession " + accession);

      Optional<EntityModel<Sample>> optionalSampleResource =
          bioSamplesWebinClient.fetchSampleResource(accession, false);

      if (optionalSampleResource.isPresent()) {
        final Sample sampleWithoutCurations = optionalSampleResource.get().getContent();

        log.info(
            "Sample with accession "
                + sampleWithoutCurations.getAccession()
                + " exists in BioSamples");

        if (sampleWithoutCurations.getRelease().isAfter(Instant.now())) {
          // private sample, make it public
          handleInsdcStatusAttribute(sampleWithoutCurations);
          handleExternalReference(sampleWithoutCurations);

          bioSamplesWebinClient
              .persistSampleResource(
                  Sample.Builder.fromSample(sampleWithoutCurations)
                      .withRelease(Instant.now())
                      .withStatus(SampleStatus.PUBLIC)
                      .build())
              .getContent();
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

  private void handleExternalReference(final Sample sampleWithoutCurations) {
    final Set<ExternalReference> externalReferences =
        sampleWithoutCurations.getExternalReferences();
    final String expectedExternalReference =
        "https://www.ebi.ac.uk/ena/browser/view/" + sampleWithoutCurations.getAccession();

    if (externalReferences.stream()
        .noneMatch(
            externalReference ->
                externalReference.getUrl().equalsIgnoreCase(expectedExternalReference))) {
      externalReferences.add(ExternalReference.build(expectedExternalReference));
      sampleWithoutCurations.getExternalReferences().addAll(externalReferences);
    }
  }

  private void handleInsdcStatusAttribute(final Sample sampleWithoutCurations) {
    sampleWithoutCurations
        .getAttributes()
        .removeIf(attribute -> attribute.getType().equals("INSDC status"));
    sampleWithoutCurations.getAttributes().add(Attribute.build("INSDC status", "public"));
    sampleWithoutCurations
        .getAttributes()
        .add(Attribute.build("ENA first public", String.valueOf(LocalDate.now())));
  }

  private ResponseEntity deleteSampleReleaseMessageInEna(final String accession) {
    final Map<String, String> params = new HashMap<>();

    params.put("biosampleAccession", accession);

    return restTemplate.exchange(
        pipelinesProperties.getWebinEraServiceSampleReleaseDelete(),
        HttpMethod.DELETE,
        new HttpEntity<>(SampleReleaseUtil.createHeaders()),
        ResponseEntity.class,
        params);
  }
}
