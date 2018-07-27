package uk.ac.ebi.biosamples.model.ga4gh;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;
import java.util.List;
import java.util.SortedMap;


public class AttributeSerializer extends StdSerializer<Ga4ghAttributes> {
    public AttributeSerializer() {
        super(Ga4ghAttributes.class);
    }

    public AttributeSerializer(JavaType type) {
        super(type);
    }

    @Override
    public void serialize(Ga4ghAttributes rawGa4ghAttributes, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
        SortedMap<String, List<Ga4ghAttributeValue>> attributes = rawGa4ghAttributes.getAttributes();
        jsonGenerator.writeStartObject();
        for (String key : attributes.keySet()) {
            jsonGenerator.writeObjectFieldStart(key);
            jsonGenerator.writeArrayFieldStart("values");
            for (Ga4ghAttributeValue value : attributes.get(key)) {
                jsonGenerator.writeStartObject();
                jsonGenerator.writeObjectField(value.getType(), value.getValue());
                jsonGenerator.writeEndObject();
            }
            jsonGenerator.writeEndArray();
            jsonGenerator.writeEndObject();
        }
        jsonGenerator.writeEndObject();
    }
}
