package uk.ac.ebi.biosamples.model.facets;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

public class FacetContentDeserializer extends StdDeserializer<FacetContent> {

    public FacetContentDeserializer() {
        this(null);
    }

    protected FacetContentDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public FacetContent deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        JsonNode root = p.readValueAsTree();
        if (root.isArray()) {
            // Has to be a LabelCountDeserializer
            List<LabelCountEntry> countEntryList = new ArrayList<>();
            Iterator<Entry<String, JsonNode>> propertiesIterator = root.fields();
            while (propertiesIterator.hasNext()) {
                Entry<String, JsonNode> node = propertiesIterator.next();

            }
        } else if (root.isObject()) {
            return null;
        }
        return null;

    }
}
