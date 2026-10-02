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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.core.model.Sample;

/**
 * Service to create a number of samples with generated names (TARACORAL + serial number), set
 * release date 100 years in the future, and bulk-accession them via BioSamples V2 in batches of
 * 200, writing the returned sample name → accession map to a file.
 */
@Service
public class BulkAccessionService {

  private static final Logger log = LoggerFactory.getLogger(BulkAccessionService.class);

  private static final String SAMPLE_NAME_PREFIX = "TARACORAL";
  private static final int BATCH_SIZE = 200;

  private final BioSamplesClient bioSamplesClient;

  public BulkAccessionService(@Qualifier("WEBINCLIENT") final BioSamplesClient bioSamplesClient) {
    this.bioSamplesClient = bioSamplesClient;
  }

  /**
   * Creates {@code count} samples with names TARACORAL1, TARACORAL2, ... and release 100 years in
   * the future, accessions them in batches of 200 via bulkAccessionV2, and writes the returned map
   * (sample name → accession) to {@code outputFilePath}.
   *
   * @param count number of samples to create and accession
   * @param outputFilePath path to the file to write "name\taccession" lines
   */
  public void createAndAccessionSamples(final int count, final String outputFilePath) {
    if (count <= 0) {
      throw new IllegalArgumentException("count must be positive, got: " + count);
    }

    final Instant releaseDate = releaseDate100YearsFromNow();
    final List<Sample> allSamples = new ArrayList<>(count);

    for (int i = 1; i <= count; i++) {
      final String name = SAMPLE_NAME_PREFIX + i;
      final Sample sample = new Sample.Builder(name).withRelease(releaseDate).build();
      allSamples.add(sample);
    }

    log.info(
        "Created {} sample objects (names {} to {}), release date {}",
        count,
        SAMPLE_NAME_PREFIX + 1,
        SAMPLE_NAME_PREFIX + count,
        releaseDate);

    final Path path = Paths.get(outputFilePath);
    int totalWritten = 0;

    for (int start = 0; start < allSamples.size(); start += BATCH_SIZE) {
      final int end = Math.min(start + BATCH_SIZE, allSamples.size());
      final List<Sample> batch = allSamples.subList(start, end);
      log.info("Accessioning batch {}-{} of {}", start + 1, end, count);

      final List<Sample> batchResult = bioSamplesClient.persistSampleResourceV2(batch);

      // Use batch order (TARACORAL1, TARACORAL2, ...) so output is ordered; Map iteration order is
      // not guaranteed.
      final List<String> batchLines = new ArrayList<>();
      for (Sample s : batchResult) {
        final String acc = s.getAccession();
        if (acc != null) {
          batchLines.add(s.getName() + "\t" + acc);
        }
      }

      try {
        // CREATE: create file if it doesn't exist (first run). APPEND: add to file (same run or
        // later runs).
        Files.write(
            path,
            batchLines,
            StandardCharsets.UTF_8,
            StandardOpenOption.CREATE,
            StandardOpenOption.APPEND);
      } catch (IOException e) {
        throw new RuntimeException(
            "Failed to write/append batch to output file: " + outputFilePath, e);
      }

      totalWritten += batchResult.size();
      log.info(
          "Appended {} entries from batch to {} (total so far: {})",
          batchResult.size(),
          outputFilePath,
          totalWritten);
    }

    log.info("Wrote {} name→accession entries to {}", totalWritten, outputFilePath);
  }

  private static Instant releaseDate100YearsFromNow() {
    return ZonedDateTime.now(ZoneOffset.UTC).plusYears(100).toInstant();
  }
}
