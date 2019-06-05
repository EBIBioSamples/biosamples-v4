package uk.ac.ebi.biosamples.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.biosamples.model.BioSchemasContext;

import java.io.IOException;

public class ContextSerializerTest {
    private static final Logger log = LoggerFactory.getLogger(ContextSerializerTest.class);

    @Test
    public void testSerialize() {
        String expectedSerializedContext = "[\"http://schema.org\",{\"OBI\":\"http://purl.obolibrary.org/obo/OBI_\"," +
                "\"biosample\":\"http://identifiers.org/biosample/\"}]";
        BioSchemasContext context = new BioSchemasContext();
        ObjectMapper mapper = new ObjectMapper();
        String serializedContext = null;
        try {
            serializedContext = mapper.writeValueAsString(context);
        } catch (IOException e) {
            log.error("Failed to serialize context");
        }

        Assert.assertEquals(expectedSerializedContext, serializedContext);
    }
}
