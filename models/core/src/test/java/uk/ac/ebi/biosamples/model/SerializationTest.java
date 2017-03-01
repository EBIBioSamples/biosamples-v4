package uk.ac.ebi.biosamples.model;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.junit4.*;

import com.fasterxml.jackson.databind.ObjectMapper;

import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.Relationship;
import uk.ac.ebi.biosamples.model.Sample;

import static org.assertj.core.api.Assertions.*;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.SortedSet;
import java.util.TreeSet;

@RunWith(SpringRunner.class)
@JsonTest
//@TestPropertySource(properties = {"spring.jackson.serialization.WRITE_DATES_AS_TIMESTAMPS=false","spring.jackson.serialization.WRITE_NULL_MAP_VALUES=false"})
public class SerializationTest {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	private JacksonTester<Sample> json;
	
    @Before
    public void setup() {
        ObjectMapper objectMapper = new ObjectMapper();
        JacksonTester.initFields(this, objectMapper);
    }

	private Sample getSimpleSample() throws URISyntaxException {
		String name = "Test Sample";
		String accession = "TEST1";
		LocalDateTime update = LocalDateTime.of(LocalDate.of(2016, 5, 5), LocalTime.of(11, 36, 57, 0));
		LocalDateTime release = LocalDateTime.of(LocalDate.of(2016, 4, 1), LocalTime.of(11, 36, 57, 0));

		SortedSet<Attribute> attributes = new TreeSet<>();
		attributes.add(Attribute.build("organism", "Homo sapiens", new URI("http://purl.obolibrary.org/obo/NCBITaxon_9606"), null));
		attributes.add(Attribute.build("age", "3", null, "year"));
		attributes.add(Attribute.build("organism part", "lung", null, null));
		attributes.add(Attribute.build("organism part", "heart", null, null));
		
		SortedSet<Relationship> relationships = new TreeSet<>();
		relationships.add(Relationship.build("derived from", "TEST2", "TEST1"));

		
		SortedSet<ExternalReference> externalReferences = new TreeSet<>();
		externalReferences.add(ExternalReference.build("http://www.google.com"));

		return Sample.build(name, accession, release, update, attributes, relationships, externalReferences);
	}

	@Test
	public void testSerialize() throws Exception {
		Sample details = getSimpleSample();

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
		
		//check that a specific attribute exists
		assertThat(this.json.readObject("/TEST1.json").getAttributes().contains(Attribute.build("organism part", "heart", null, null)));
	}

	@Test
	public void testRoundTrip() throws Exception {
		Sample sample = getSimpleSample();
		String json = this.json.write(sample).getJson();
		InputStream inputStream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
		Sample sampleRedux = this.json.readObject(inputStream);
		String jsonRedux = this.json.write(sampleRedux).getJson();
		
		System.out.println(json);
		System.out.println(jsonRedux);

        BufferedReader br = new BufferedReader(new InputStreamReader(new ClassPathResource("/TEST1.json").getInputStream()),1024);
        StringBuilder stringBuilder = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            stringBuilder.append(line).append('\n');
        }
        br.close();
        String jsonFile = stringBuilder.toString();
		
		assertThat(sample.equals(sampleRedux));
		assertThat(sample.equals(jsonFile));
		assertThat(json.equals(jsonRedux));
		assertThat(json.equals(jsonFile));
	}
	
	@SpringBootConfiguration
	public static class TestConfig {
		
	}
	
}
