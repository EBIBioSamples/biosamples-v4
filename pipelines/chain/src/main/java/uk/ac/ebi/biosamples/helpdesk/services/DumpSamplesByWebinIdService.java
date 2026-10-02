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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.hateoas.EntityModel;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.core.model.Sample;
import uk.ac.ebi.biosamples.core.model.filter.AuthenticationFilter;
import uk.ac.ebi.biosamples.core.model.filter.Filter;

/** Dumps samples matching a webinSubmissionAccountId to an output file. */
@Service
public class DumpSamplesByWebinIdService {
  private static final Logger log = LoggerFactory.getLogger(DumpSamplesByWebinIdService.class);

  private final BioSamplesClient webinClient;
  private final ObjectMapper objectMapper;

  public DumpSamplesByWebinIdService(
      @Qualifier("WEBINCLIENT") final BioSamplesClient webinClient,
      final ObjectMapper objectMapper) {
    this.webinClient = webinClient;
    this.objectMapper = objectMapper;
  }

  /**
   * Queries samples by webinSubmissionAccountId and writes matching samples to a file.
   *
   * @param webinId webin submission account id to search
   * @param outputFile output file path
   */
  public void dumpSamplesByWebinId(final String webinId, final String outputFile) {
    final Filter webinFilter = new AuthenticationFilter.Builder(webinId).build();
    final Instant now = Instant.now();
    long dumpedCount = 0;
    final Iterable<EntityModel<Sample>> samples =
        webinClient.fetchSampleResourceAllWithoutCuration("", List.of(webinFilter));

    try (BufferedWriter writer =
        Files.newBufferedWriter(
            Paths.get(outputFile),
            StandardCharsets.UTF_8,
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING)) {
      for (EntityModel<Sample> model : samples) {
        final Sample s = model.getContent();
        if (s == null) {
          continue;
        }

        final Instant release = s.getRelease();
        if (release != null && release.isBefore(now)) {
          writer.write(toJson(s));
          writer.newLine();
          dumpedCount++;

          if (dumpedCount % 10000 == 0) {
            log.info(
                "Progress for webinSubmissionAccountId {}: dumped {} samples (release < now)",
                webinId,
                dumpedCount);
          }
        }
      }
    } catch (IOException e) {
      throw new RuntimeException("Failed writing output file: " + outputFile, e);
    }

    log.info(
        "Dumped {} samples for webinSubmissionAccountId {} to {}",
        dumpedCount,
        webinId,
        outputFile);
  }

  private String toJson(final Sample sample) {
    try {
      return objectMapper.writeValueAsString(sample);
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Failed to serialize sample to JSON: " + sample.getAccession(), e);
    }
  }
}
