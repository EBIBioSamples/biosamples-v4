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
package uk.ac.ebi.biosamples;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.client.utils.ClientProperties;
import uk.ac.ebi.biosamples.utils.IntegrationTestFailException;

@Component
public class FileDownloadIntegration extends AbstractIntegration {
  private final Logger log = LoggerFactory.getLogger(getClass());
  private final ClientProperties clientProperties;

  public FileDownloadIntegration(
      final BioSamplesClient client, final ClientProperties clientProperties) {
    super(client);
    this.clientProperties = clientProperties;
  }

  @Override
  protected void phaseOne() {
    // nothing to do here
  }

  @Override
  protected void phaseTwo() {
    // nothing to do here
  }

  @Override
  protected void phaseThree() {
    final String sampleDownloadUrl = clientProperties.getBiosamplesClientUri() + "/download";
    try (final ZipInputStream inputStream =
        new ZipInputStream(new URL(sampleDownloadUrl).openStream())) {
      final ZipEntry entry = inputStream.getNextEntry();
      if (entry == null || !"samples.json".equals(entry.getName())) {
        throw new IntegrationTestFailException(
            "Could not download zipped samples.json", Phase.THREE);
      }

      final StringWriter writer = new StringWriter();
      IOUtils.copy(inputStream, writer, Charset.defaultCharset());
      final JsonNode samples = new ObjectMapper().readTree(writer.toString());
      if (!samples.isArray()) {
        throw new IntegrationTestFailException("Invalid format in samples.json", Phase.THREE);
      }
    } catch (final IOException e) {
      // TODO: @Isuru to check please!
      /*throw new IntegrationTestFailException("Could not download search results", Phase.THREE);*/
    }
  }

  @Override
  protected void phaseFour() {
    // nothing to do here
  }

  @Override
  protected void phaseFive() {
    // nothing to do here
  }

  @Override
  protected void phaseSix() {}
}
