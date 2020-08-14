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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import java.io.IOException;
import uk.ac.ebi.biosamples.model.BioSchemasContext;

public class ContextSerializer extends StdSerializer<BioSchemasContext> {

  public ContextSerializer() {
    super(BioSchemasContext.class);
  }

  @Override
  public void serialize(
      BioSchemasContext bioSchemasContext,
      JsonGenerator jsonGenerator,
      SerializerProvider serializerProvider)
      throws IOException {

    // Write the @base field -> Not sure why this is need, but following UNIPROT convention
    // jsonGenerator.writeStartObject();
    // jsonGenerator.writeStringField("@base", bioSchemasContext.getBaseContext().toString());
    // jsonGenerator.writeEndObject();

    jsonGenerator.writeStartArray();

    // Write the schema.org base namespace
    jsonGenerator.writeString(bioSchemasContext.getSchemaOrgContext().toString());

    // Write all the other contexts
    if (!bioSchemasContext.getOtherContexts().isEmpty()) {
      jsonGenerator.writeObject(bioSchemasContext.getOtherContexts());
    }

    jsonGenerator.writeEndArray();
  }
}
