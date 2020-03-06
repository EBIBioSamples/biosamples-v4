package uk.ac.ebi.biosamples.service;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import uk.ac.ebi.biosamples.model.Accession;

import java.io.IOException;

public class AccessionSerializer extends StdSerializer<Accession> {
    protected AccessionSerializer() {
        super(Accession.class);
    }

    @Override
    public void serialize(Accession accession, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeString(accession.getId());
    }
}