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
package uk.ac.ebi.biosamples.ncbi;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.hateoas.EntityModel;
import uk.ac.ebi.biosamples.BioSamplesProperties;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.client.service.AapClientService;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.service.SampleValidator;

public class MockBioSamplesClient extends BioSamplesClient {

  private Logger log = LoggerFactory.getLogger(getClass());

  private static PrintWriter printWriter;

  private static FileWriter fileWriter;

  private ObjectMapper objectMapper;

  private long count = 0;

  public MockBioSamplesClient(
      URI uri,
      URI uriV2,
      RestTemplateBuilder restTemplateBuilder,
      SampleValidator sampleValidator,
      AapClientService aapClientService,
      BioSamplesProperties bioSamplesProperties,
      ObjectMapper objectMapper) {
    super(uri, uriV2, restTemplateBuilder, sampleValidator, aapClientService, bioSamplesProperties);
    this.objectMapper = objectMapper;
    try {
      fileWriter = new FileWriter("ncbi-import.json");
      printWriter = new PrintWriter(fileWriter);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void logSample(Sample sample) {
    count++;
    String sampleJson = "";
    try {
      objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
      sampleJson = objectMapper.writeValueAsString(sample);
    } catch (JsonProcessingException e) {
      e.printStackTrace();
    }
    printWriter.printf("%s\n", sampleJson);
    if (count % 500 == 0) {
      log.info("Recorded " + count + " samples");
    }
  }

  @Override
  public EntityModel<Sample> persistSampleResource(Sample sample) {
    logSample(sample);
    return Mockito.mock(EntityModel.class);
  }

  public void finalize() {
    try {
      fileWriter.close();
      printWriter.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
