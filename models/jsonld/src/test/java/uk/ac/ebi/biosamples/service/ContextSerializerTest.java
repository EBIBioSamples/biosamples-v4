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
package uk.ac.ebi.biosamples.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.biosamples.model.BioSchemasContext;

public class ContextSerializerTest {
  private static final Logger log = LoggerFactory.getLogger(ContextSerializerTest.class);

  @Test
  public void testSerialize() {
    String expectedSerializedContext =
        "[\"http://schema.org\",{\"OBI\":\"http://purl.obolibrary.org/obo/OBI_\","
            + "\"biosample\":\"http://identifiers.org/biosample/\"}]";
    BioSchemasContext context = new BioSchemasContext();
    ObjectMapper mapper = new ObjectMapper();
    String serializedContext = null;
    try {
      serializedContext = mapper.writeValueAsString(context);
    } catch (IOException e) {
      log.error("Failed to serialize context");
    }

    Assert.assertEquals(expectedSerializedContext, serializedContext);
  }
}
