package uk.ac.ebi.biosamples.models;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.*;

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@RunWith(SpringRunner.class)
@JsonTest
public class SerializationTest {

	private JacksonTester<SimpleSample> json;

	private SimpleSample getSimpleSample() throws URISyntaxException {
		String name = "Test Sample";
		String accession = "TEST1";
		LocalDate update = LocalDate.of(2016, 5, 5);
		LocalDate release = LocalDate.of(2016, 4, 1);

		Map<String, Set<String>> keyValues = new HashMap<>();
		Map<String, Map<String, String>> ontologyTerms = new HashMap<>();
		Map<String, Map<String, String>> units = new HashMap<>();

		keyValues.put("organism", new HashSet<>());
		keyValues.get("organism").add("Homo sapiens");
		ontologyTerms.put("organism", new HashMap<>());
		ontologyTerms.get("organism").put("Homo sapiens", "http://purl.obolibrary.org/obo/NCBITaxon_9606");

		keyValues.put("age", new HashSet<>());
		keyValues.get("age").add("3");
		units.put("age", new HashMap<>());
		units.get("age").put("3", "year");

		keyValues.put("organism part", new HashSet<>());
		keyValues.get("organism part").add("lung");
		keyValues.get("organism part").add("heart");

		return SimpleSample.createFrom(name, accession, update, release, keyValues, ontologyTerms, units);
	}

	@Test
	public void testSerialize() throws Exception {
		SimpleSample details = getSimpleSample();

		System.out.println(this.json.write(details).getJson());

		// Use JSON path based assertions
		assertThat(this.json.write(details)).hasJsonPathStringValue("@.accession");
		assertThat(this.json.write(details)).extractingJsonPathStringValue("@.accession").isEqualTo("TEST1");

		// Assert against a `.json` file in the same package as the test
		assertThat(this.json.write(details)).isEqualToJson("/TEST1.json");
	}

	@Test
	public void testDeserialize() throws Exception {
		// Use JSON path based assertions
		assertThat(this.json.readObject("/TEST1.json").getName()).isEqualTo("Test Sample");
		assertThat(this.json.readObject("/TEST1.json").getAccession()).isEqualTo("TEST1");
		// Assert against a `.json` file
		assertThat(this.json.readObject("/TEST1.json")).isEqualTo(getSimpleSample());
	}
	
	@Configuration
	public static class TestConfig {
		@Bean
		public ObjectMapper getObjectMapper() {
			ObjectMapper om = new ObjectMapper();
			return om;
		}
	}

}
