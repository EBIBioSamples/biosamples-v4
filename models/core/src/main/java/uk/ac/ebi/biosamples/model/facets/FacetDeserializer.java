package uk.ac.ebi.biosamples.model.facets;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

public class FacetDeserializer extends StdDeserializer<Facet> {


    public FacetDeserializer() {
        super((Class<?>) null);
    }

    @Override
    public Facet deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        JsonNode root = p.readValueAsTree();
        Iterator<Map.Entry<String, JsonNode>> propertiesIterator = root.fields();
        FacetType type = null;
        String label = null;
        int count = -1;
        FacetContent content = null;
        while (propertiesIterator.hasNext()) {
            Map.Entry<String, JsonNode> node = propertiesIterator.next();
            switch(node.getKey()) {
                case "label":
                    label = node.getValue().asText();
                    break;
                case "count":
                    count = node.getValue().asInt();
                    break;
                case "type":
                    type = FacetType.ofFacetName(node.getValue().asText());
                    break;
                case "content":
                    JavaType jacksonType = ctxt.getTypeFactory().constructType(FacetContent.class);
                    JsonDeserializer<?> deserializer = ctxt.findRootValueDeserializer(jacksonType);
                    JsonParser nodeParser = root.traverse(ctxt.getParser().getCodec());
                    nodeParser.nextToken();
                    content =  (FacetContent) deserializer.deserialize(nodeParser, ctxt);
                    break;

            }
        }
        return FacetFactory.build(type, label, count, content);
    }
}
