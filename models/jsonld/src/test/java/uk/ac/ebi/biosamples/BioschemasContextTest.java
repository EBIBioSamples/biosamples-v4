package uk.ac.ebi.biosamples.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@JsonTest
//@TestPropertySource(properties = {"spring.jackson.serialization.WRITE_DATES_AS_TIMESTAMPS=false","spring.jackson.serialization.WRITE_NULL_MAP_VALUES=false"})
@TestPropertySource(properties={"spring.jackson.serialization.INDENT_OUTPUT=true"})
public class BioschemasContextTest {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	private JacksonTester<BioSchemasContext> json;

//	@Test
	public void testDeserialize() throws Exception {
        String contextJson = "[" +
									" \"http://schema.org\", " +
									" { \"@base\": \"http://schema.org\" }, " +
									" { \"Sample\": { \"@id\": \"http://purl.obolibrary.org/obo/OBI_0000747\" } } ]";

        BioSchemasContext context = new ObjectMapper()
				.readerFor(BioSchemasContext.class)
				.readValue(contextJson);

		// Use JSON path based assertions
		assertThat(context.getBaseContext()).isEqualTo(URI.create("http://schema.org"));
	}

	@SpringBootConfiguration
	public static class TestConfig {

	}

}
