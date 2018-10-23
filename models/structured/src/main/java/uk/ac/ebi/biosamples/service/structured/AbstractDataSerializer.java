package uk.ac.ebi.biosamples.service.structured;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import uk.ac.ebi.biosamples.model.structured.AbstractData;

import java.io.IOException;
import java.util.Set;

public class AbstractDataSerializer extends StdSerializer<Set> {

    protected AbstractDataSerializer() {
        super(Set.class);
    }

    @Override
    public void serialize(Set rawData, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        Set<AbstractData> abstractDataSet = (Set<AbstractData>) rawData;

        jsonGenerator.writeStartObject();
        for (AbstractData data: abstractDataSet) {
            jsonGenerator.writeFieldName(data.getDataType().toString());
            jsonGenerator.writeStartObject();
            jsonGenerator.writeStringField("schema", data.getSchema().toString());
            jsonGenerator.writeObjectField("data", data.getStructuredData());
            jsonGenerator.writeEndObject();

        }
        jsonGenerator.writeEndObject();

    }
}
