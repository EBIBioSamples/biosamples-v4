package uk.ac.ebi.biosamples.model.ga4gh;


import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.util.*;


public class AttributeDeserializer extends StdDeserializer<Ga4ghAttributes> {

    public AttributeDeserializer() {
        super(Ga4ghAttributes.class);
    }

    @Override
    public Ga4ghAttributes deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
        Ga4ghAttributes ga4ghAttributes = new Ga4ghAttributes();
        ObjectCodec codec = jsonParser.getCodec();
        JsonNode node = codec.readTree(jsonParser);
        return deserializeAttributes(node);
    }

    private Ga4ghAttributes deserializeAttributes(JsonNode node) {
        Iterator<Map.Entry<String, JsonNode>> fieldsIterator = node.fields();
        SortedMap<String, List<Ga4ghAttributeValue>> attributesFields = new TreeMap<>();
        while (fieldsIterator.hasNext()) {
            Map.Entry<String, JsonNode> field = fieldsIterator.next();
            String key = field.getKey();
            attributesFields.put(key, deserializeAttributeList(field.getValue().get("values")));
        }
        Ga4ghAttributes ga4ghAttributes = new Ga4ghAttributes();
        ga4ghAttributes.setAttributes(attributesFields);
        return ga4ghAttributes;
    }

    private List<Ga4ghAttributeValue> deserializeAttributeList(JsonNode node) {
        List<Ga4ghAttributeValue> ga4ghAttributeValues = new ArrayList<>();
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
                        ga4ghAttributeValues.add(new Ga4ghAttributeValue(value.textValue()));
                        break;
                    case "int64_value":
                        ga4ghAttributeValues.add(new Ga4ghAttributeValue(value.longValue()));
                        break;
                    case "bool_value":
                        ga4ghAttributeValues.add(new Ga4ghAttributeValue(value.booleanValue()));
                        break;
                    case "double_value":
                        ga4ghAttributeValues.add(new Ga4ghAttributeValue(value.doubleValue()));
                        break;
                    case "external_identifier":
                        try {
                            ga4ghAttributeValues.add(new Ga4ghAttributeValue(mapper.readValue(value.asText(), ExternalIdentifier.class)));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    case "ontology_term":
                        try {
                            ga4ghAttributeValues.add(new Ga4ghAttributeValue(mapper.readValue(value.asText(), OntologyTerm.class)));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    case "experiment":
                        try {
                            ga4ghAttributeValues.add(new Ga4ghAttributeValue(mapper.readValue(value.asText(), Experiment.class)));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    case "analysis":
                        try {
                            ga4ghAttributeValues.add(new Ga4ghAttributeValue(mapper.readValue(value.asText(), Analysis.class)));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    case "null_value":
                        ga4ghAttributeValues.add(new Ga4ghAttributeValue(null));
                    case "attributes":
                        ga4ghAttributeValues.add(new Ga4ghAttributeValue(deserializeAttributes(value)));
                    case "attribute_list":
                        List<Ga4ghAttributeValue> values = deserializeAttributeList(value);
                        //TODO add comments or reformat code to more underatandable
                        if (values != null && values.size() > 0) {
                            ga4ghAttributeValues.add(new Ga4ghAttributeValue());
                        }
                }
            }
        }
        return ga4ghAttributeValues;
    }


}
