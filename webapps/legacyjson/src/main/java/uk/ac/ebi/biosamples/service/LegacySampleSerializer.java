package uk.ac.ebi.biosamples.service;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import uk.ac.ebi.biosamples.model.Sample;

import java.io.IOException;

@Component
public class LegacySampleSerializer extends StdSerializer<Sample> {

    Logger log = LoggerFactory.getLogger(getClass());

    public LegacySampleSerializer() {
        this(Sample.class);
    }

    protected LegacySampleSerializer(Class<Sample> t) {
        super(t);
    }

    @Override
    public void serialize(Sample sample, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        log.info("Passing in " + getClass().getCanonicalName());
        jsonGenerator.writeStartObject();
        jsonGenerator.writeStringField("accession", sample.getAccession());
        jsonGenerator.writeEndObject();
    }
}
