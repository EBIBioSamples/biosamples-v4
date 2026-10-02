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
package uk.ac.ebi.biosamples.helpdesk.services;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.hateoas.EntityModel;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.core.model.Attribute;
import uk.ac.ebi.biosamples.core.model.Sample;
import uk.ac.ebi.biosamples.core.model.SampleStatus;

@Service
@Slf4j
public class SamplesStatusUpdater {
  @Autowired
  @Qualifier("WEBINCLIENT")
  private BioSamplesClient webinClient;

  public List<String> parseFileAndGetSampleAccessionList(final String file) {
    final List<String> accessions = new ArrayList<>();

    try (final BufferedReader bufferedReader = new BufferedReader(new FileReader(file))) {
      String line;

      while ((line = bufferedReader.readLine()) != null) {
        accessions.add(line);
      }
    } catch (FileNotFoundException e) {
      throw new RuntimeException(e);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    return accessions;
  }

  public void processSamples(final List<String> accessions, final SampleStatus toMakeStatus) {
    accessions.forEach(accession -> processSample(accession, toMakeStatus));
  }

  private void processSample(final String accession, final SampleStatus toMakeStatus) {
    log.info("Handling " + accession);

    Optional<EntityModel<Sample>> optionalSampleEntityModel =
        webinClient.fetchSampleResource(accession, false);

    if (optionalSampleEntityModel.isPresent()) {
      final Sample sample = optionalSampleEntityModel.get().getContent();

      handleSample(sample, toMakeStatus);
    } else {
      log.info("Not found " + accession);
    }
  }

  private void handleSample(final Sample sample, final SampleStatus toMakeStatus) {
    final String accession = sample.getAccession();

    Sample updatedSample = null;

    if (toMakeStatus == SampleStatus.PRIVATE) {
      if (sample.getRelease().isBefore(Instant.now())) {
        updatedSample =
            Sample.Builder.fromSample(sample)
                .withRelease(
                    Instant.ofEpochSecond(
                        LocalDateTime.now(ZoneOffset.UTC)
                            .plusYears(100)
                            .toEpochSecond(ZoneOffset.UTC)))
                .withStatus(SampleStatus.PRIVATE)
                .build();
      } else {
        log.info("{} is already private", accession);
      }
    } else if (toMakeStatus == SampleStatus.PUBLIC) {
      final Instant now = Instant.now();
      final Set<Attribute> attributes = new HashSet<>(sample.getAttributes());
      final boolean releaseNeedsFix = sample.getRelease().isAfter(now);
      final boolean statusNeedsFix = sample.getStatus() != SampleStatus.PUBLIC;
      final boolean insdcExists =
          attributes.stream().anyMatch(a -> a.getType().equalsIgnoreCase("INSDC status"));
      final boolean insdcNeedsFix =
          attributes.stream()
              .anyMatch(
                  a ->
                      a.getType().equalsIgnoreCase("INSDC status")
                          && !a.getValue().equalsIgnoreCase("public"));

      if (releaseNeedsFix || statusNeedsFix || insdcNeedsFix) {
        // only replace INSDC status if it already exists
        if (insdcExists) {
          attributes.removeIf(a -> a.getType().equalsIgnoreCase("INSDC status"));
          attributes.add(Attribute.build("INSDC status", "public"));
        }

        updatedSample =
            Sample.Builder.fromSample(sample)
                .withRelease(releaseNeedsFix ? now : sample.getRelease())
                .withAttributes(attributes)
                .withStatus(SampleStatus.PUBLIC)
                .build();

      } else {
        log.info("{} is already public", accession);
      }
    } else if (toMakeStatus == SampleStatus.SUPPRESSED) {
      final Set<Attribute> attributes = sample.getAttributes();

      attributes.removeIf(attribute -> attribute.getType().equalsIgnoreCase("INSDC status"));
      attributes.add(Attribute.build("INSDC status", "suppressed"));

      updatedSample =
          Sample.Builder.fromSample(sample)
              .withAttributes(attributes)
              .withStatus(SampleStatus.SUPPRESSED)
              .build();
    }

    if (updatedSample != null) {
      webinClient.persistSampleResource(updatedSample);

      log.info("{} is updated to status {}", accession, toMakeStatus.name());
    }
  }
}
