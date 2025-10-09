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
package uk.ac.ebi.biosamples.core.service.structured;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import java.io.IOException;
import java.util.Set;
import uk.ac.ebi.biosamples.core.model.structured.AbstractData;

public class AbstractDataSerializer extends StdSerializer<Set> {

  protected AbstractDataSerializer() {
    super(Set.class);
  }

  @Override
  public void serialize(
      final Set rawData,
      final JsonGenerator jsonGenerator,
      final SerializerProvider serializerProvider)
      throws IOException {
    final Set<AbstractData> abstractDataSet = (Set<AbstractData>) rawData;

    jsonGenerator.writeStartObject();

    for (final AbstractData data : abstractDataSet) {
      jsonGenerator.writeFieldName(data.getDataType().toString());
      jsonGenerator.writeStartObject();
      jsonGenerator.writeStringField("schema", data.getSchema().toString());
      jsonGenerator.writeStringField("domain", data.getDomain());
      jsonGenerator.writeObjectField("data", data.getStructuredData());
      jsonGenerator.writeEndObject();
    }

    jsonGenerator.writeEndObject();
  }
}
