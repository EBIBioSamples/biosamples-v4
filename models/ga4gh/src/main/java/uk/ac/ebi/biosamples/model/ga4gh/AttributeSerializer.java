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
package uk.ac.ebi.biosamples.model.ga4gh;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import java.io.IOException;
import java.util.List;
import java.util.SortedMap;

public class AttributeSerializer extends StdSerializer<Ga4ghAttributes> {
  public AttributeSerializer() {
    super(Ga4ghAttributes.class);
  }

  public AttributeSerializer(JavaType type) {
    super(type);
  }

  @Override
  public void serialize(
      Ga4ghAttributes rawAttributes,
      JsonGenerator jsonGenerator,
      SerializerProvider serializerProvider)
      throws IOException {
    SortedMap<String, List<AttributeValue>> attributes = rawAttributes.getAttributes();
    jsonGenerator.writeStartObject();
    for (String key : attributes.keySet()) {
      jsonGenerator.writeObjectFieldStart(key);
      jsonGenerator.writeArrayFieldStart("values");
      for (AttributeValue value : attributes.get(key)) {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeObjectField(value.getType(), value.getValue());
        jsonGenerator.writeEndObject();
      }
      jsonGenerator.writeEndArray();
      jsonGenerator.writeEndObject();
    }
    jsonGenerator.writeEndObject();
  }
}
