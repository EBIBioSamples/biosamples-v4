package uk.ac.ebi.biosamples.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.comparator.CustomComparator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@JsonTest
//@TestPropertySource(properties = {"spring.jackson.serialization.WRITE_DATES_AS_TIMESTAMPS=false","spring.jackson.serialization.WRITE_NULL_MAP_VALUES=false"})
@TestPropertySource(properties = {"spring.jackson.serialization.INDENT_OUTPUT=true"})
public class BioschemasContextTest {

    private Logger log = LoggerFactory.getLogger(this.getClass());

    private JacksonTester<BioSchemasContext> json;

    @Test
    public void testDeserialize() throws Exception {
        String contextJson = "[" +
                "\"http://schema.org\"," +
                "{\"OBI\":\"http://purl.obolibrary.org/obo/OBI_\",\"biosample\":\"http://identifiers.org/biosample\"}" +
                "]";

        BioSchemasContext context = new ObjectMapper()
                .readerFor(BioSchemasContext.class)
                .readValue(contextJson);

        // Use JSON path based assertions
        assertThat(context.getSchemaOrgContext()).isEqualTo(URI.create("http://schema.org"));
    }

    @Test
    public void testSerialize() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        BioSchemasContext context = new BioSchemasContext();
        context.addOtherContexts("OBI", URI.create("http://purl.obolibrary.org/obo/OBI_"));
        context.addOtherContexts("biosample", URI.create("http://identifiers.org/biosample"));
        String actualJson = mapper.writeValueAsString(context);

        String expectedJson = "[" +
                "\"http://schema.org\"," +
                "{\"OBI\":\"http://purl.obolibrary.org/obo/OBI_\",\"biosample\":\"http://identifiers.org/biosample\"}" +
                "]";

        JSONAssert.assertEquals(expectedJson, actualJson, new CustomComparator(JSONCompareMode.LENIENT,
                new Customization("metaData.created", (o1, o2) -> true)));
    }

    @SpringBootConfiguration
    public static class TestConfig {

    }

}
