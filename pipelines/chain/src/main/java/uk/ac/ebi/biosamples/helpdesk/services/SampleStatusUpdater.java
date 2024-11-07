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
import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.filter.AttributeFilter;
import uk.ac.ebi.biosamples.model.filter.Filter;

@Service
@Slf4j
public class SampleStatusUpdater {
  @Autowired private BioSamplesClient aapClient;

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

  public void makeFilteredSamplesPrivate() {
    final List<Sample> samples = new ArrayList<>();
    final Filter projectFilter =
        new AttributeFilter.Builder("project").withValue("MorPhiC").build();

    final Iterable<EntityModel<Sample>> samplesIterable =
        webinClient.fetchSampleResourceAll(Collections.singletonList(projectFilter));

    samplesIterable.forEach(
        sampleEntityModel -> {
          final Sample sample = sampleEntityModel.getContent();

          if (sample.getRelease().isBefore(Instant.now())) {
            samples.add(sample);
          }
        });

    if (samples.size() == 169) {
      log.info("Accession count matches expectations - process samples");

      samples.forEach(
          sample -> {
            final Set<Attribute> attributes = sample.getAttributes();
            final Attribute projectAttribute =
                attributes.stream()
                    .filter(attribute -> attribute.getType().equals("project"))
                    .toList()
                    .get(0);

            attributes.remove(projectAttribute);
            attributes.add(Attribute.build("project", "MorPhiC-Test"));

            sample =
                Sample.Builder.fromSample(sample)
                    .withAttributes(attributes)
                    .withRelease(
                        Instant.ofEpochSecond(
                            LocalDateTime.now(ZoneOffset.UTC)
                                .plusYears(100)
                                .toEpochSecond(ZoneOffset.UTC)))
                    .build();

            log.info("Persisting sample " + sample.getAccession());

            webinClient.persistSampleResource(sample);
          });
    }
  }

  public void processSamples(final List<String> accessions) {
    accessions.forEach(accession -> processSample(accession));
  }

  private void processSample(final String accession) {
    log.info("Handling " + accession);

    Optional<EntityModel<Sample>> optionalSampleEntityModel =
        aapClient.fetchSampleResource(accession, Optional.of(Collections.singletonList("")));

    if (optionalSampleEntityModel.isEmpty()) {
      optionalSampleEntityModel =
          webinClient.fetchSampleResource(accession, Optional.of(Collections.singletonList("")));
    }

    if (optionalSampleEntityModel.isPresent()) {
      final Sample sample = optionalSampleEntityModel.get().getContent();

      handleSample(sample);
    } else {
      log.info("Not found " + accession);
    }
  }

  private void handleSample(final Sample sample) {
    if (sample.getRelease().isAfter(Instant.now())) {
      log.info("Sample " + sample.getAccession() + " is already private, no action required");
    } else {
      log.info("Sample " + sample.getAccession() + " is public, making private");

      final Sample updatedSample =
          Sample.Builder.fromSample(sample)
              .withRelease(
                  Instant.ofEpochSecond(
                      LocalDateTime.now(ZoneOffset.UTC)
                          .plusYears(100)
                          .toEpochSecond(ZoneOffset.UTC)))
              .build();

      if (updatedSample.getWebinSubmissionAccountId() != null) {
        webinClient.persistSampleResource(updatedSample);
      } else {
        aapClient.persistSampleResource(updatedSample);
      }
    }
  }
}
