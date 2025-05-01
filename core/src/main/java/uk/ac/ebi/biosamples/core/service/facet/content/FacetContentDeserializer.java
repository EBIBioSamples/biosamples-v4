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
package uk.ac.ebi.biosamples.core.service.facet.content;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import uk.ac.ebi.biosamples.core.model.facet.content.FacetContent;
import uk.ac.ebi.biosamples.core.model.facet.content.LabelCountEntry;
import uk.ac.ebi.biosamples.core.model.facet.content.LabelCountListContent;

public class FacetContentDeserializer extends JsonDeserializer<FacetContent> {
  @Override
  public FacetContent deserialize(final JsonParser p, final DeserializationContext ctxt)
      throws IOException {
    final JsonNode root = p.readValueAsTree();
    if (root.isArray()) {
      // If the json is an array, I'm assuming it to be a LabelCountListContent facet content
      return parseLabelCountListContent(root);
    }

    throw new JsonParseException(p, "Unable to parse facet content");
  }

  /**
   * Deserialize the Json to a LabelCountListContent
   *
   * @param jsonArray the Json Array representing LabelCountListContent
   * @return LabelCountListContent
   */
  private LabelCountListContent parseLabelCountListContent(final JsonNode jsonArray) {
    final List<LabelCountEntry> labelCountList = new ArrayList<>();
    for (final JsonNode labelCountJsonEntry : jsonArray) {
      final String label = labelCountJsonEntry.get("label").asText();
      final Long count = labelCountJsonEntry.get("count").asLong();

      labelCountList.add(LabelCountEntry.build(label, count));
    }

    return new LabelCountListContent(labelCountList);
  }
}
