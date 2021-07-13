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
import java.net.URI;
import java.util.Iterator;
import uk.ac.ebi.biosamples.model.structured.AbstractData;
import uk.ac.ebi.biosamples.model.structured.HistologyEntry;
import uk.ac.ebi.biosamples.model.structured.StructuredDataType;
import uk.ac.ebi.biosamples.model.structured.StructuredTable;
import uk.ac.ebi.biosamples.model.structured.amr.AMREntry;
import uk.ac.ebi.biosamples.model.structured.amr.AMRTable;

public class AbstractDataDeserializer extends StdDeserializer<AbstractData> {
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
    URI schema = URI.create(rootNode.get("schema").asText());
    StructuredDataType type =
        StructuredDataType.valueOf(rootNode.get("type").asText().toUpperCase());
    JsonNode domain = rootNode.get("domain");
    JsonNode webinSubmissionAccountId = rootNode.get("webinSubmissionAccountId");
    JsonNode content = rootNode.get("content");
    String domainStr = (domain != null && !domain.isNull()) ? domain.asText() : null;
    String webinIdStr =
        (webinSubmissionAccountId != null && !webinSubmissionAccountId.isNull())
            ? webinSubmissionAccountId.asText()
            : null;

    // Deserialize the object based on the datatype
    if (type == StructuredDataType.AMR) {
      AMRTable.Builder tableBuilder = new AMRTable.Builder(schema, domainStr, webinIdStr);
      for (Iterator<JsonNode> it = content.elements(); it.hasNext(); ) {
        JsonNode amrRowObject = it.next();
        AMREntry entry = this.objectMapper.treeToValue(amrRowObject, AMREntry.class);
        tableBuilder.addEntry(entry);
      }
      return tableBuilder.build();
    } else if (type == StructuredDataType.CHICKEN_DATA
        || type == StructuredDataType.HISTOLOGY_MARKERS
        || type == StructuredDataType.MOLECULAR_MARKERS
        || type == StructuredDataType.FATTY_ACIDS) {
      StructuredTable.Builder<HistologyEntry> tableBuilder =
          new StructuredTable.Builder<>(schema, domainStr, webinIdStr, type);
      for (Iterator<JsonNode> it = content.elements(); it.hasNext(); ) {
        JsonNode amrRowObject = it.next();
        HistologyEntry entry = this.objectMapper.treeToValue(amrRowObject, HistologyEntry.class);
        tableBuilder.addEntry(entry);
      }
      return tableBuilder.build();
    }

    return null;
  }
}
