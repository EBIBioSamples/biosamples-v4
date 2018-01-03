package uk.ac.ebi.biosamples.model.facet.content;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

public class FacetContentDeserializer extends JsonDeserializer<FacetContent> {



    @Override
    public FacetContent deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonParseException {
        JsonNode root = p.readValueAsTree();
        if (root.isArray()) {
            // If the json is an array, I'm assuming it to be a LabelCountListContent facet content
            return parseLabelCountListContent(root);
        }
        throw new JsonParseException(p, "Unable to parse facet content");
    }

    /**
     * Deserialize the Json to a LabelCountListContent
     * @param jsonArray the Json Array representing LabelCountListContent
     * @return LabelCountListContent
     */
    private LabelCountListContent parseLabelCountListContent(JsonNode jsonArray) {
        List<LabelCountEntry> labelCountList = new ArrayList<>();
        for (JsonNode labelCountJsonEntry: jsonArray ) {
            String label = labelCountJsonEntry.get("label").asText();
            Long count = labelCountJsonEntry.get("count").asLong();
            labelCountList.add(LabelCountEntry.build(label, count));
        }
        return new LabelCountListContent(labelCountList);
    }
}
