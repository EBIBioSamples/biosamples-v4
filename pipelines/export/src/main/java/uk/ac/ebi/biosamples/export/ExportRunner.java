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
package uk.ac.ebi.biosamples.export;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.hateoas.EntityModel;
import org.springframework.stereotype.Component;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.Sample;

@Component
public class ExportRunner implements ApplicationRunner {

  private final Logger log = LoggerFactory.getLogger(getClass());
  private final BioSamplesClient bioSamplesClient;
  private final ObjectMapper objectMapper;

  public ExportRunner(BioSamplesClient bioSamplesClient, ObjectMapper objectMapper) {
    // ensure the client is public
    if (bioSamplesClient.getPublicClient().isPresent()) {
      this.bioSamplesClient = bioSamplesClient.getPublicClient().get();
    } else {
      this.bioSamplesClient = bioSamplesClient;
    }
    this.objectMapper = objectMapper;
  }

  @Override
  public void run(ApplicationArguments args) {
    String jsonSampleFilename = args.getNonOptionArgs().get(0);
    boolean removeCurations = args.getOptionValues("curationdomain") != null;
    long oldTime = System.nanoTime();
    int sampleCount = 0;
    boolean isPassed = true;
    try {
      boolean first = true;
      try (Writer jsonSampleWriter =
          args.getOptionValues("gzip") == null
              ? new OutputStreamWriter(
                  new FileOutputStream(jsonSampleFilename), StandardCharsets.UTF_8)
              : new OutputStreamWriter(
                  new GZIPOutputStream(new FileOutputStream(jsonSampleFilename)),
                  StandardCharsets.UTF_8)) {
        jsonSampleWriter.write("[\n");
        for (EntityModel<Sample> sampleResource :
            bioSamplesClient.fetchSampleResourceAll(!removeCurations)) {
          log.trace("Handling " + sampleResource);
          Sample sample = sampleResource.getContent();
          if (sample == null) {
            throw new RuntimeException("Sample should not be null");
          }
          if (!first) {
            jsonSampleWriter.write(",\n");
          }
          jsonSampleWriter.write(objectMapper.writeValueAsString(sample));
          first = false;
          sampleCount += 1;
        }
        jsonSampleWriter.write("\n]");
      }
    } catch (final Exception e) {
      isPassed = false;
    } finally {
      long elapsed = System.nanoTime() - oldTime;
      log.info("Exported " + sampleCount + " samples in " + (elapsed / 1000000000L) + "s");
    }
  }
}
