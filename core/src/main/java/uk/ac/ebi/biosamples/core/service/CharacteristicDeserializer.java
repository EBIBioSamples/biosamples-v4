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
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.biosamples.core.model.Attribute;

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
public class CharacteristicDeserializer extends StdDeserializer<SortedSet> {
  private final Logger log = LoggerFactory.getLogger(getClass());

  public CharacteristicDeserializer() {
    this(SortedSet.class);
  }

  private CharacteristicDeserializer(final Class<SortedSet> t) {
    super(t);
  }

  @Override
  public SortedSet deserialize(final JsonParser p, final DeserializationContext ctxt)
      throws IOException {
    final SortedSet<Attribute> attributes = new TreeSet<>();
    final Map<String, List<LegacyAttribute>> characteristics =
        p.readValueAs(new TypeReference<Map<String, List<LegacyAttribute>>>() {});

    for (final String type : characteristics.keySet()) {
      for (final LegacyAttribute legacy : characteristics.get(type)) {
        attributes.add(
            Attribute.build(type, legacy.text, legacy.tag, legacy.ontologyTerms, legacy.unit));
      }
    }

    return attributes;
  }

  /**
   * This internal class is used for easier deserializtion with jackson. The fields match the field
   * names in the JSON and therefore should not be changed!
   *
   * @author faulcon
   */
  private static class LegacyAttribute {
    public String text;
    public List<String> ontologyTerms;
    public String tag;
    public String unit;
  }
}
