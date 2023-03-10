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

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;
import java.net.URI;
import uk.ac.ebi.biosamples.model.BioSchemasContext;

public class ContextDeserializer extends StdDeserializer<BioSchemasContext> {

  protected ContextDeserializer() {
    super(BioSchemasContext.class);
  }

  @Override
  public BioSchemasContext deserialize(
      final JsonParser jsonParser, final DeserializationContext deserializationContext)
      throws IOException {
    final BioSchemasContext context = new BioSchemasContext();

    JsonToken currentToken = jsonParser.getCurrentToken();
    if (currentToken.equals(JsonToken.START_ARRAY)) {
      while (jsonParser.hasCurrentToken() && !currentToken.equals(JsonToken.END_ARRAY)) {
        currentToken = jsonParser.nextToken();

        if (currentToken.equals(JsonToken.VALUE_STRING)
            && !jsonParser.getValueAsString().equals("http://schema.org")) {
          // This is not the link to schema.org we expect to see
          throw new JsonParseException(
              jsonParser, "BioSchemasContext should contain a single schema.org entry string");
        } else if (currentToken.equals(JsonToken.START_OBJECT)) {
          while (jsonParser.hasCurrentToken() && !currentToken.equals(JsonToken.END_OBJECT)) {
            currentToken = jsonParser.nextToken();
            if (currentToken.equals(JsonToken.FIELD_NAME)) {
              context.addOtherContexts(
                  jsonParser.getValueAsString(), URI.create(jsonParser.nextTextValue()));
            }
          }
        }
      }
    } else {
      throw new JsonParseException(jsonParser, "BioSchemasContext should be an array");
    }

    return context;
  }
}
