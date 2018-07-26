package uk.ac.ebi.biosamples.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import org.assertj.core.util.Lists;
import org.junit.Before;
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
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.SortedSet;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@JsonTest
//@TestPropertySource(properties = {"spring.jackson.serialization.WRITE_DATES_AS_TIMESTAMPS=false","spring.jackson.serialization.WRITE_NULL_MAP_VALUES=false"})
@TestPropertySource(properties={"spring.jackson.serialization.INDENT_OUTPUT=true"})
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
		String domain = "abcde12345";
		Instant update = Instant.parse("2016-05-05T11:36:57.00Z");
		Instant release = Instant.parse("2016-04-01T11:36:57.00Z");

		SortedSet<Attribute> attributes = new TreeSet<>();
		attributes.add(Attribute.build("organism", "Homo sapiens", "http://purl.obolibrary.org/obo/NCBITaxon_9606", null));
		attributes.add(Attribute.build("age", "3", Lists.emptyList(), "year"));
		attributes.add(Attribute.build("organism part", "lung"));
		attributes.add(Attribute.build("organism part", "heart"));
		attributes.add(Attribute.build("sex", "female", Sets.newHashSet("http://purl.obolibrary.org/obo/PATO_0000383","http://www.ebi.ac.uk/efo/EFO_0001265"), null));

		SortedSet<Relationship> relationships = new TreeSet<>();
		relationships.add(Relationship.build("TEST1", "derived from", "TEST2"));
		
		SortedSet<ExternalReference> externalReferences = new TreeSet<>();
		externalReferences.add(ExternalReference.build("http://www.google.com"));

		SortedSet<Organization> organizations = new TreeSet<>();
//		organizations.add(Organization.build("Jo Bloggs Inc", "user", "help@jobloggs.com", "http://www.jobloggs.com"));
		organizations.add(new Organization.Builder()
				.name("Jo Bloggs Inc")
				.role("user")
				.email("help@jobloggs.com")
				.url("http://www.jobloggs.com")
				.build());

		SortedSet<Contact> contacts = new TreeSet<>();
//		contacts.add(Contact.build("Joe Bloggs","Jo Bloggs Inc", "http://www.jobloggs.com/joe"));
		contacts.add(new Contact.Builder()
				.firstName("Joe")
				.lastName("Bloggs")
//                .affiliation("Jo Bloggs Inc")
//				.url("http://www.jobloggs.com/joe")
				.name("Joe Bloggs")
				.role("Submitter")
				.email("jobloggs@joblogs.com")
				.build());

		SortedSet<Publication> publications = new TreeSet<>();
//		publications.add(Publication.build("10.1093/nar/gkt1081", "24265224"));
		publications.add(new Publication.Builder()
                .doi("10.1093/nar/gkt1081")
                .pubmed_id("24265224")
				.build());

//		return Sample.build(name, accession, domain, release, update, attributes, relationships, externalReferences, organizations, contacts, publications);
        return new Sample.Builder(name, accession).withDomain(domain)
				.withRelease(release).withUpdate(update)
				.withAttributes(attributes).withRelationships(relationships).withExternalReferences(externalReferences)
                .withOrganizations(organizations).withContacts(contacts).withPublications(publications)
				.build();
	}

	@Test
	public void testSerialize() throws Exception {
		Sample details = getSimpleSample();

		log.info(this.json.write(details).getJson());

		// Use JSON path based assertions
		assertThat(this.json.write(details)).hasJsonPathStringValue("@.accession");
		assertThat(this.json.write(details)).extractingJsonPathStringValue("@.accession").isEqualTo("TEST1");

		// Assert against a `.json` file in the same package as the test
		assertThat(this.json.write(details)).isEqualToJson("/TEST1.json");
	}

	@Test
	public void testDeserialize() throws Exception {
		Sample fileSample = this.json.readObject("/TEST1.json");
		Sample simpleSample = getSimpleSample();
		log.info("fileSample = "+fileSample);
		log.info("simpleSample = "+simpleSample);
		// Use JSON path based assertions
		assertThat(fileSample.getName()).isEqualTo("Test Sample");
		assertThat(fileSample.getAccession()).isEqualTo("TEST1");
		// Assert against a `.json` file
		assertThat(fileSample).isEqualTo(simpleSample);
		
		//check that a specific attribute exists
		assertThat(fileSample.getCharacteristics().contains(Attribute.build("organism part", "heart")));
	}

	@Test
	public void testRoundTrip() throws Exception {
		Sample sample = getSimpleSample();
		log.info("roundTrip sample = "+sample);
		
		String json = this.json.write(sample).getJson();
		log.info("roundTrip json = "+json);
		
		InputStream inputStream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
		Sample sampleRedux = this.json.readObject(inputStream);
		log.info("roundTrip sampleRedux = "+sampleRedux);
		
		String jsonRedux = this.json.write(sampleRedux).getJson();
		log.info("roundTrip jsonRedux = "+jsonRedux);


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
