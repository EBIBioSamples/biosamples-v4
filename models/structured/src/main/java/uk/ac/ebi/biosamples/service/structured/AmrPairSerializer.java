package uk.ac.ebi.biosamples.service.structured;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import uk.ac.ebi.biosamples.model.structured.amr.AmrPair;

import java.io.IOException;

public class AmrPairSerializer extends StdSerializer<AmrPair> {
    public AmrPairSerializer() {
        super(AmrPair.class);
    }

    public AmrPairSerializer(Class<AmrPair> t) {
        super(t);
    }

    @Override
    public void serialize(AmrPair amrPair, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeStartObject();
        gen.writeStringField("value", amrPair.getValue());
        gen.writeStringField("iri", amrPair.getIri());
        gen.writeEndObject();
    }
}
