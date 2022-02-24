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
package uk.ac.ebi.biosamples.service.structured;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.biosamples.model.structured.AbstractData;

public class AbstractDataDeserializer extends StdDeserializer<AbstractData> {
  private final Logger log = LoggerFactory.getLogger(getClass());
  private final ObjectMapper objectMapper = new ObjectMapper();

  protected AbstractDataDeserializer() {
    super(AbstractData.class);
  }

  protected AbstractDataDeserializer(Class<AbstractData> t) {
    super(t);
  }

  @Override
  public AbstractData deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
    JsonNode rootNode = p.getCodec().readTree(p);
    JsonNode content = rootNode.get("content");

    if (content.elements().hasNext()) {
      log.warn("Trying to deserialize deprecated structured data. Ignoring the input.");
    }

    return null;
  }
}
