package uk.ac.ebi.biosamples.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.biosamples.model.BioSchemasContext;

import java.io.IOException;
import java.net.URI;

public class ContextDeserializerTest {
    private static final Logger log = LoggerFactory.getLogger(ContextDeserializerTest.class);

    @Test
    public void testSerialize_schemaOrgContext() {
        String contextString = "[\"http://schema.org\",{\"OBI\":\"http://purl.obolibrary.org/obo/OBI_\"," +
                "\"biosample\":\"http://identifiers.org/biosample\"}]";
        BioSchemasContext expectedContext = new BioSchemasContext();
        ObjectMapper mapper = new ObjectMapper();
        BioSchemasContext context = null;
        try {
            context = mapper.readValue(contextString, BioSchemasContext.class);
        } catch (IOException e) {
            log.error("Failed to deserialize context");
            Assert.fail();
        }

        Assert.assertEquals(expectedContext.getSchemaOrgContext(), context.getSchemaOrgContext());
    }

    @Test
    public void testSerialize_otherContext() {
        String contextString = "[\"http://schema.org\",{\"OBI\":\"http://purl.obolibrary.org/obo/OBI_\"," +
                "\"biosample\":\"http://identifiers.org/biosample\",\"ebi\":\"https://www.ebi.ac.uk/biosamples/\"}]";
        BioSchemasContext expectedContext = new BioSchemasContext();
        expectedContext.addOtherContexts("ebi", URI.create("https://www.ebi.ac.uk/biosamples/"));
        ObjectMapper mapper = new ObjectMapper();
        BioSchemasContext context = null;
        try {
            context = mapper.readValue(contextString, BioSchemasContext.class);
        } catch (IOException e) {
            log.error("Failed to deserialize context");
            Assert.fail();
        }

        Assert.assertEquals(expectedContext.getOtherContexts().size(), context.getOtherContexts().size());
    }
}
