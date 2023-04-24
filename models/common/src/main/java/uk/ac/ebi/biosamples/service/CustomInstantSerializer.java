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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

public class CustomInstantSerializer extends StdSerializer<Instant> {

  public CustomInstantSerializer() {
    this(null);
  }

  public CustomInstantSerializer(final Class<Instant> t) {
    super(t);
  }

  @Override
  public void serialize(final Instant value, final JsonGenerator gen, final SerializerProvider arg2)
      throws IOException {
    gen.writeString(DateTimeFormatter.ISO_INSTANT.format(value));
  }
}
