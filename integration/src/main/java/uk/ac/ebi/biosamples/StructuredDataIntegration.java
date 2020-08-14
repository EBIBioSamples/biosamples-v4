/*
* Copyright 2019 EMBL - European Bioinformatics Institute
* Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
* file except in compliance with the License. You may obtain a copy of the License at
* http://www.apache.org/licenses/LICENSE-2.0
* Unless required by applicable law or agreed to in writing, software distributed under the
* License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
* CONDITIONS OF ANY KIND, either express or implied. See the License for the
* specific language governing permissions and limitations under the License.
*/
package uk.ac.ebi.biosamples;

import static org.junit.Assert.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.hateoas.Resource;
import org.springframework.stereotype.Component;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.structured.AbstractData;
import uk.ac.ebi.biosamples.model.structured.HistologyEntry;
import uk.ac.ebi.biosamples.model.structured.StructuredDataType;
import uk.ac.ebi.biosamples.model.structured.StructuredTable;
import uk.ac.ebi.biosamples.utils.IntegrationTestFailException;
import uk.ac.ebi.biosamples.utils.TestUtilities;

@Component
public class StructuredDataIntegration extends AbstractIntegration {
  private final Logger log = LoggerFactory.getLogger(this.getClass());
  private ObjectMapper mapper;

  public StructuredDataIntegration(BioSamplesClient client) {
    super(client);
    this.mapper = new ObjectMapper();
  }

  @Override
  protected void phaseOne() {
    String json = TestUtilities.readFileAsString("structured_data_sample.json");
    Sample sample;
    try {
      sample = mapper.readValue(json, Sample.class);
    } catch (IOException e) {
      throw new IntegrationTestFailException(
          "An error occurred while converting json to Sample class" + e, Phase.ONE);
    }

    Resource<Sample> submittedSample = this.client.persistSampleResource(sample);
    if (!sample.equals(submittedSample.getContent())) {
      throw new IntegrationTestFailException(
          "Expected: " + sample + ", found: " + submittedSample.getContent(), Phase.ONE);
    }
  }

  @Override
  protected void phaseTwo() {
    Optional<Resource<Sample>> sampleResource =
        client.fetchSampleResource("StructuredDataIntegration_sample_1");
    if (sampleResource.isEmpty()) {
      throw new IntegrationTestFailException(
          "Expected structured data sample not present", Phase.TWO);
    }

    Sample sample = sampleResource.get().getContent();
    log.info("Checking sample has histology data");
    assertEquals(3, sample.getData().size());

    for (AbstractData data : sample.getData()) {
      if (data.getDataType() == StructuredDataType.CHICKEN_DATA) {
        StructuredTable<HistologyEntry> chickenData = (StructuredTable<HistologyEntry>) data;
        assertEquals(1, chickenData.getStructuredData().size());

        log.info("Verifying structured data content");
        Optional<HistologyEntry> optionalEntry =
            chickenData
                .getStructuredData()
                .parallelStream()
                .filter(entry -> entry.getMarker().getValue().equalsIgnoreCase("Cortisol"))
                .findFirst();
        if (optionalEntry.isEmpty()) {
          throw new IntegrationTestFailException(
              "Structured data content verification failed", Phase.TWO);
        }

        HistologyEntry entry = optionalEntry.get();
        assertEquals(entry.getMarker().getValue(), "Cortisol");
        assertEquals(entry.getMeasurement().getValue(), "0.000");
        assertEquals(entry.getMeasurementUnits().getValue(), "log pg/mg feather");
        assertEquals(entry.getPartner().getValue(), "IRTA");
      }
    }
  }

  @Override
  protected void phaseThree() {
    // skip
  }

  @Override
  protected void phaseFour() {
    // skip
  }

  @Override
  protected void phaseFive() {
    // skip
  }
}
