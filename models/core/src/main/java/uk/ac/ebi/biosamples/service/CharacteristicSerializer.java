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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import java.io.IOException;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.biosamples.model.Attribute;

/*

"characteristics": {
    "material": [
      {
        "text": "specimen from organism",
        "ontologyTerms": [
          "http://purl.obolibrary.org/obo/OBI_0001479"
        ]
      }
    ],
    "specimenCollectionDate": [
      {
        "text": "2013-05",
        "unit": "YYYY-MM"
      }
    ],
 */
public class CharacteristicSerializer extends StdSerializer<SortedSet> {
  private Logger log = LoggerFactory.getLogger(this.getClass());

  public CharacteristicSerializer() {
    this(SortedSet.class);
  }

  public CharacteristicSerializer(Class<SortedSet> t) {
    super(t);
  }

  @Override
  public void serialize(SortedSet attributesRaw, JsonGenerator gen, SerializerProvider arg2)
      throws IOException, JsonProcessingException {
    SortedSet<Attribute> attributes = (SortedSet<Attribute>) attributesRaw;

    gen.writeStartObject();

    SortedMap<String, ArrayListValuedHashMap<String, Attribute>> attributeMap = new TreeMap<>();

    if (attributes != null && attributes.size() > 0) {
      for (Attribute attribute : attributes) {
        if (!attributeMap.containsKey(attribute.getType())) {
          attributeMap.put(attribute.getType(), new ArrayListValuedHashMap<>());
        }

        attributeMap
            .get(attribute.getType())
            .put(
                attribute.getValue(),
                Attribute.build(
                    attribute.getType(),
                    attribute.getValue(),
                    attribute.getTag(),
                    attribute.getIri(),
                    attribute.getUnit()));
      }

      for (String type : attributeMap.keySet()) {
        gen.writeArrayFieldStart(type);

        for (String value : attributeMap.get(type).keySet()) {
          for (Attribute attr : attributeMap.get(type).get(value)) {
            gen.writeStartObject();
            gen.writeStringField("text", value);

            if (attr.getIri() != null && attr.getIri().size() > 0) {
              gen.writeArrayFieldStart("ontologyTerms");

              for (String iri : attr.getIri()) {
                gen.writeString(iri);
              }

              gen.writeEndArray();
            }

            if (attr.getUnit() != null) {
              gen.writeStringField("unit", attr.getUnit());
            }

            if (attr.getTag() != null) {
              gen.writeStringField("tag", attr.getTag());
            }

            gen.writeEndObject();
          }
        }

        gen.writeEndArray();
      }
    }

    gen.writeEndObject();
  }
}
