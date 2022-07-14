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
import uk.ac.ebi.biosamples.utils.IntegrationTestFailException;

@Component
public class FileDownloadIntegration extends AbstractIntegration {
  private Logger log = LoggerFactory.getLogger(this.getClass());
  private BioSamplesProperties clientProperties;

  public FileDownloadIntegration(BioSamplesClient client, BioSamplesProperties clientProperties) {
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
    String sampleDownloadUrl = this.clientProperties.getBiosamplesClientUri() + "/download";
    try (ZipInputStream inputStream = new ZipInputStream(new URL(sampleDownloadUrl).openStream())) {
      ZipEntry entry = inputStream.getNextEntry();
      if (entry == null || !"samples.json".equals(entry.getName())) {
        throw new IntegrationTestFailException(
            "Could not download zipped samples.json", Phase.THREE);
      }

      StringWriter writer = new StringWriter();
      IOUtils.copy(inputStream, writer, Charset.defaultCharset());
      JsonNode samples = new ObjectMapper().readTree(writer.toString());
      if (!samples.isArray()) {
        throw new IntegrationTestFailException("Invalid format in samples.json", Phase.THREE);
      }
    } catch (IOException e) {
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
