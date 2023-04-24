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
import java.net.URI;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.biosamples.model.BioSchemasContext;

public class ContextDeserializerTest {
  private static final Logger log = LoggerFactory.getLogger(ContextDeserializerTest.class);

  @Test
  public void testSerialize_schemaOrgContext() {
    final String contextString =
        "[\"http://schema.org\",{\"OBI\":\"http://purl.obolibrary.org/obo/OBI_\","
            + "\"biosample\":\"http://identifiers.org/biosample\"}]";
    final BioSchemasContext expectedContext = new BioSchemasContext();
    final ObjectMapper mapper = new ObjectMapper();
    BioSchemasContext context = null;
    try {
      context = mapper.readValue(contextString, BioSchemasContext.class);
    } catch (final IOException e) {
      log.error("Failed to deserialize context");
      Assert.fail();
    }

    Assert.assertEquals(expectedContext.getSchemaOrgContext(), context.getSchemaOrgContext());
  }

  @Test
  public void testSerialize_otherContext() {
    final String contextString =
        "[\"http://schema.org\",{\"OBI\":\"http://purl.obolibrary.org/obo/OBI_\","
            + "\"biosample\":\"http://identifiers.org/biosample\",\"ebi\":\"https://www.ebi.ac.uk/biosamples/\"}]";
    final BioSchemasContext expectedContext = new BioSchemasContext();
    expectedContext.addOtherContexts("ebi", URI.create("https://www.ebi.ac.uk/biosamples/"));
    final ObjectMapper mapper = new ObjectMapper();
    BioSchemasContext context = null;
    try {
      context = mapper.readValue(contextString, BioSchemasContext.class);
    } catch (final IOException e) {
      log.error("Failed to deserialize context");
      Assert.fail();
    }

    Assert.assertEquals(
        expectedContext.getOtherContexts().size(), context.getOtherContexts().size());
  }
}
