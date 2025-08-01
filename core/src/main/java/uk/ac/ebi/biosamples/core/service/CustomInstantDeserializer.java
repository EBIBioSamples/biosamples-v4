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
package uk.ac.ebi.biosamples.core.service;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeParseException;

public class CustomInstantDeserializer extends StdDeserializer<Instant> {

  public CustomInstantDeserializer() {
    this(null);
  }

  private CustomInstantDeserializer(final Class<Instant> t) {
    super(t);
  }

  @Override
  public Instant deserialize(final JsonParser jsonparser, final DeserializationContext ctxt)
      throws IOException, JsonProcessingException {
    String date = jsonparser.getText();
    // TODO remove this hack
    if (!date.endsWith("Z")) {
      date = date + "Z";
    }
    try {
      return Instant.parse(date);
    } catch (final DateTimeParseException e) {
      throw new RuntimeException(e);
    }
  }
}
