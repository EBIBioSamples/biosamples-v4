package uk.ac.ebi.biosamples.model.ga4gh_model;


import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import uk.ac.ebi.biosamples.model.ga4gh_model.*;
import java.io.IOException;
import java.util.*;


public class AttributeDeserializer extends StdDeserializer<Attributes> {

    public AttributeDeserializer() {
        super(Attributes.class);
    }

    @Override
    public Attributes deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
        Attributes attributes = new Attributes();
        ObjectCodec codec = jsonParser.getCodec();
        JsonNode node = codec.readTree(jsonParser);
        return deserializeAttributes(node);
    }

    private Attributes deserializeAttributes(JsonNode node) {
        Iterator<Map.Entry<String, JsonNode>> fieldsIterator = node.fields();
        SortedMap<String, List<AttributeValue>> attributesFields = new TreeMap<>();
        while (fieldsIterator.hasNext()) {
            Map.Entry<String, JsonNode> field = fieldsIterator.next();
            String key = field.getKey();
            attributesFields.put(key, deserializeAttributeList(field.getValue().get("values")));
        }
        Attributes attributes = new Attributes();
        attributes.setAttributes(attributesFields);
        return attributes;
    }

    private List<AttributeValue> deserializeAttributeList(JsonNode node) {
        List<AttributeValue> attributeValues = new ArrayList<>();
        Iterator<JsonNode> attributeObjects = node.iterator();
        ObjectMapper mapper = new ObjectMapper();
        while (attributeObjects.hasNext()) {
            JsonNode currentNode = attributeObjects.next();
            Iterator<String> names = currentNode.fieldNames();
            while (names.hasNext()) {
                String fieldName = names.next();
                JsonNode value = currentNode.get(fieldName);
                switch (fieldName) {
                    case "string_value":
                        attributeValues.add(new AttributeValue(value.textValue()));
                        break;
                    case "int64_value":
                        attributeValues.add(new AttributeValue(value.longValue()));
                        break;
                    case "bool_value":
                        attributeValues.add(new AttributeValue(value.booleanValue()));
                        break;
                    case "double_value":
                        attributeValues.add(new AttributeValue(value.doubleValue()));
                        break;
                    case "external_identifier":
                        try {
                            attributeValues.add(new AttributeValue(mapper.readValue(value.asText(), ExternalIdentifier.class)));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    case "ontology_term":
                        try {
                            attributeValues.add(new AttributeValue(mapper.readValue(value.asText(), OntologyTerm.class)));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    case "experiment":
                        try {
                            attributeValues.add(new AttributeValue(mapper.readValue(value.asText(), Experiment.class)));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    case "analysis":
                        try {
                            attributeValues.add(new AttributeValue(mapper.readValue(value.asText(), Analysis.class)));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    case "null_value":
                        attributeValues.add(new AttributeValue(null));
                    case "attributes":
                        attributeValues.add(new AttributeValue(deserializeAttributes(value)));
                    case "attribute_list":
                        List<AttributeValue> values = deserializeAttributeList(value);
                        //TODO add comments or reformat code to more underatandable
                        if (values != null && values.size() > 0) {
                            attributeValues.add(new AttributeValue());
                        }
                }
            }
        }
        return attributeValues;
    }


}
