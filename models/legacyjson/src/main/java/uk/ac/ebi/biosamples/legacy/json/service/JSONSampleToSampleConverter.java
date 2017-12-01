package uk.ac.ebi.biosamples.legacy.json.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import org.springframework.core.convert.converter.Converter;
import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.Sample;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Map.Entry;


public class JSONSampleToSampleConverter implements Converter<String, Sample> {

    private final ObjectMapper mapper;

    public JSONSampleToSampleConverter() {
        this.mapper = new ObjectMapper();
    }


    @Override
    public Sample convert(String source) {
        String accession = JsonPath.read(source, "$.accession");
        String sampleName = JsonPath.read(source, "$.name");
        String updateDate = JsonPath.read(source,"$.updateDate");
        String releaseDate = JsonPath.read(source,"$.releaseDate");
        String description = JsonPath.read(source, "$.description");

        SortedSet<Attribute> attributes = getAttributes(source);

        Sample.Builder sampleBuilder = new Sample.Builder(accession, sampleName)
                .withUpdateDate(updateDate)
                .withReleaseDate(releaseDate)
                .withAttribute(Attribute.build("description", description));

        attributes.forEach(sampleBuilder::withAttribute);
        return sampleBuilder.build();

    }


    private SortedSet<Attribute> getAttributes(String json) {
        LinkedHashMap<String, Object> characteristics = JsonPath.read(json, "$.characteristics");
        SortedSet<Attribute> attributes = new TreeSet<>();

        for(Entry<String, Object> crt: characteristics.entrySet()) {
            String attributeType = crt.getKey();
            List<Map<String, Object>> values = (List<Map<String, Object>>) crt.getValue();
            for(Map<String,Object> value: values) {

                String attributeValue = (String) value.get("text");
                List<String> ontologyTerms = (List<String>) value.get("ontologyTerms");
                String unit = (String) value.get("unit");

                attributes.add(Attribute.build(attributeType, attributeValue, ontologyTerms, unit));

            }


        }

//        try {

//            JsonNode jsonAttributes = mapper.readTree(characteristics);
//            Iterator<Entry<String, JsonNode>> attributesIterator = jsonAttributes.fields();
//            while(attributesIterator.hasNext()) {
//                String attributeType = null;
//                String attributeValue = null;
//                List<String> ontologyTerms = null;
//                List<String> units = null;
//
//                Entry<String, JsonNode> attributeEntry = attributesIterator.next();
//                attributeType = attributeEntry.getKey();
//
//                Iterator<JsonNode> attributeValuesIterator = attributeEntry.getValue().elements();
//                while(attributeValuesIterator.hasNext()) {
//                    JsonNode attributeValueNode = attributeValuesIterator.next();
//                    attributeValue = attributeValueNode.get("text").textValue();
//
//                    JsonNode ontologyTermsNode = attributeValueNode.path("ontologyTerms");
//                    if (!ontologyTermsNode.isMissingNode()) {
//                        ontologyTerms = Stream.generate(ontologyTermsNode.elements()::next)
//                                .map(JsonNode::textValue).collect(Collectors.toList());
//                    }
//                    attributes.add(Attribute.build(attributeType, attributeValue, ontologyTerms, null));
//                    JsonNode unitNode = attributeValueNode.path("unit");
//                    if (!unitNode.isMissingNode())
//                }
//            }
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

        return attributes;

    }
}