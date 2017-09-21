package uk.ac.ebi.biosamples.models;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URISyntaxException;
import java.time.Instant;
import java.util.SortedSet;
import java.util.TreeSet;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringRunner;

import com.fasterxml.jackson.databind.ObjectMapper;

import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.mongo.model.MongoExternalReference;
import uk.ac.ebi.biosamples.mongo.model.MongoRelationship;
import uk.ac.ebi.biosamples.mongo.model.MongoSample;

@RunWith(SpringRunner.class)
@JsonTest
public class MongoSerializationTest {

	private Logger log = LoggerFactory.getLogger(getClass());

	private JacksonTester<MongoSample> json;
	
    @Before
    public void setup() {
        ObjectMapper objectMapper = new ObjectMapper();
        JacksonTester.initFields(this, objectMapper);
    }

	private MongoSample getMongoSample() throws URISyntaxException {
		String name = "Test Sample";
		String accession = "TEST1";
		Instant update = Instant.parse("2016-05-05T11:36:57.00Z");
		Instant release = Instant.parse("2016-04-01T11:36:57.00Z");

		SortedSet<Attribute> attributes = new TreeSet<>();
		attributes.add(Attribute.build("organism", "Homo sapiens", "http://purl.obolibrary.org/obo/NCBITaxon_9606", null));
		attributes.add(Attribute.build("age", "3", null, "year"));
		attributes.add(Attribute.build("organism part", "lung", null, null));
		attributes.add(Attribute.build("organism part", "heart", null, null));
		
		SortedSet<MongoRelationship> relationships = new TreeSet<>();
		relationships.add(MongoRelationship.build("TEST1", "derived from", "TEST2"));
		
		SortedSet<MongoExternalReference> externalReferences = new TreeSet<>();
		externalReferences.add(MongoExternalReference.build("http://www.google.com"));

		return MongoSample.build(name, accession, "foozit", release, update, attributes, relationships, externalReferences);
	}

	@Test
	public void testSerialize() throws Exception {
		MongoSample details = getMongoSample();

		System.out.println(this.json.write(details).getJson());

		// Use JSON path based assertions
		assertThat(this.json.write(details)).hasJsonPathStringValue("@.accession");
		assertThat(this.json.write(details)).extractingJsonPathStringValue("@.accession").isEqualTo("TEST1");

		// Assert against a `.json` file in the same package as the test
		log.info("testSerialize() "+this.json.write(details).getJson());
		assertThat(this.json.write(details)).isEqualToJson("/TEST1.json");
	}

	@Test
	public void testDeserialize() throws Exception {
		// Use JSON path based assertions
		assertThat(this.json.readObject("/TEST1.json").getName()).isEqualTo("Test Sample");
		assertThat(this.json.readObject("/TEST1.json").getAccession()).isEqualTo("TEST1");
		// Assert against a `.json` file
		assertThat(this.json.readObject("/TEST1.json")).isEqualTo(getMongoSample());
	}
	
	@Configuration
	public static class TestConfig {
		
	}

}
