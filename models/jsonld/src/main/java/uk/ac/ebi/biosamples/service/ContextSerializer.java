package uk.ac.ebi.biosamples.service;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import uk.ac.ebi.biosamples.model.BioSchemasContext;

import java.io.IOException;

public class ContextSerializer extends StdSerializer<BioSchemasContext> {

    public ContextSerializer() {
        super(BioSchemasContext.class);
    }

    @Override
    public void serialize(BioSchemasContext bioSchemasContext, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {

        // Write the @base field -> Not sure why this is need, but following UNIPROT convention
        //jsonGenerator.writeStartObject();
        //jsonGenerator.writeStringField("@base", bioSchemasContext.getBaseContext().toString());
        //jsonGenerator.writeEndObject();

        jsonGenerator.writeStartArray();

        // Write the schema.org base namespace
        jsonGenerator.writeString(bioSchemasContext.getSchemaOrgContext().toString());

        // Write all the other contexts
        if (!bioSchemasContext.getOtherContexts().isEmpty()) {
            jsonGenerator.writeObject(bioSchemasContext.getOtherContexts());
        }

        jsonGenerator.writeEndArray();
    }
}
