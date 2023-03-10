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
package uk.ac.ebi.biosamples.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.JsonLDDataRecord;
import uk.ac.ebi.biosamples.model.Sample;

public class SampleToJsonLDSampleRecordConverterTest {
  private static final Logger log =
      LoggerFactory.getLogger(SampleToJsonLDSampleRecordConverterTest.class);

  @Test
  public void testConvert() {
    final Sample sample = getSample();

    final SampleToJsonLDSampleRecordConverter converter = new SampleToJsonLDSampleRecordConverter();
    final JsonLDDataRecord record = converter.convert(sample);

    Assert.assertEquals(
        sample.getAttributes().first().getIri().first(),
        record.getMainEntity().getAdditionalProperties().get(0).getValueReference().get(0).getId());
  }

  @Test
  public void testSerializeDeserialize() {
    final Sample sample = getSample();
    final SampleToJsonLDSampleRecordConverter converter = new SampleToJsonLDSampleRecordConverter();
    final JsonLDDataRecord record = converter.convert(sample);

    JsonLDDataRecord deserializedRecord = null;
    final ObjectMapper mapper = new ObjectMapper();
    try {
      final String serializedRecord =
          mapper.writerWithDefaultPrettyPrinter().writeValueAsString(record);
      deserializedRecord = mapper.readValue(serializedRecord, JsonLDDataRecord.class);
    } catch (final IOException e) {
      log.error("Failed to serialize JsonLD record");
      e.printStackTrace();
      Assert.fail();
    }

    Assert.assertEquals(
        record.getContext().getOtherContexts().size(),
        deserializedRecord.getContext().getOtherContexts().size());
  }

  private Sample getSample() {
    return new Sample.Builder("FakeName", "FakeAccession")
        .withDomain("test.fake.domain")
        .addAttribute(
            Attribute.build(
                "Organism", "Homo Sapiens", "http://purl.obolibrary.org/obo/NCBITaxon_9606", null))
        .build();
  }
}
