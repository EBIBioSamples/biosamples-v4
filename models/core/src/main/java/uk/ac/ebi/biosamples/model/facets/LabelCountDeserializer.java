package uk.ac.ebi.biosamples.model.facets;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.util.*;

public class LabelCountDeserializer extends StdDeserializer<LabelCountEntry>{

    private final List<String> mandatoryFields = Arrays.asList("label", "count");

    public LabelCountDeserializer() {
        this(null);
    }

    protected LabelCountDeserializer(Class<?> vc) {
        super(vc);
    }

    private Map<String, Boolean> initializeCheckingTable() {
        Map<String, Boolean> stateTable = new HashMap<>();
        mandatoryFields.forEach(field -> stateTable.put(field, false));
        return stateTable;
    }



    @Override
    public LabelCountEntry deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        Map<String, Boolean> checkingTable = initializeCheckingTable();
        JsonNode root = p.readValueAsTree();
        Iterator<Map.Entry<String, JsonNode>> propertiesIterator = root.fields();
        while (propertiesIterator.hasNext()) {
            Map.Entry<String, JsonNode> node = propertiesIterator.next();
            checkingTable.computeIfPresent(node.getKey(), (key, value) -> value = true);
        }

        if (checkingTable.containsValue(Boolean.FALSE)) {
            return null;
        } else {
            return LabelCountEntry.build(root.get("label").asText(), root.get("count").asLong());
        }
    }
}
