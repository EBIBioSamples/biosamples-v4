package uk.ac.ebi.biosamples.service.ga4gh_services;

import com.example.simple_biosamples_client.models.ga4ghmetadata.AttributeValue;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;

public class AttributeValueSerializer extends StdSerializer<AttributeValue> {
    public AttributeValueSerializer() {
        super(AttributeValue.class);
    }


    @Override
    public void serialize(AttributeValue value, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeObjectField(value.getType(), value.getValue());
        jsonGenerator.writeEndObject();
    }

}
