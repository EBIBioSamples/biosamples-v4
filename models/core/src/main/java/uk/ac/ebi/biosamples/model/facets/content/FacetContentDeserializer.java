package uk.ac.ebi.biosamples.model.facets.content;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FacetContentDeserializer extends JsonDeserializer<FacetContent> {


    @Override
    public FacetContent deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonParseException {
        JsonNode root = p.readValueAsTree();
        if (root.isArray()) {
            List<LabelCountEntry> labelCountList = new ArrayList<>();
            for (JsonNode labelCountJsonEntry: root) {
                String label = labelCountJsonEntry.get("label").asText();
                Long count = labelCountJsonEntry.get("count").asLong();
                labelCountList.add(LabelCountEntry.build(label, count));
            }
            return new LabelCountListContent(labelCountList);
        }
        throw new JsonParseException(p, "Unable to parse facet content");
    }
}
