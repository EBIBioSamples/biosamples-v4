package uk.ac.ebi.biosamples;

import com.google.common.collect.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.client.Traverson;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.*;

import javax.annotation.PreDestroy;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;

@Component
public class RestIntegration extends AbstractIntegration {

	private Logger log = LoggerFactory.getLogger(this.getClass());
	private final RestTemplate restTemplate;
	private BioSamplesProperties clientProperties;
	private final BioSamplesClient annonymousClient;
	
	
	public RestIntegration(BioSamplesClient client, RestTemplateBuilder restTemplateBuilder, BioSamplesProperties clientProperties) {
		super(client);
		this.restTemplate = restTemplateBuilder.build();
		this.clientProperties = clientProperties;
		this.annonymousClient = new BioSamplesClient(this.clientProperties.getBiosamplesClientUri(), restTemplateBuilder, null, null, clientProperties);
	}

	@Override
	protected void phaseOne() {
		Sample sampleTest1 = getSampleTest1();
		// get and check that nothing exists already
		Optional<Resource<Sample>> optional = client.fetchSampleResource(sampleTest1.getAccession());
		if (optional.isPresent()) {
			throw new RuntimeException("Found existing "+sampleTest1.getAccession());
		}

		// put a sample
		Resource<Sample> resource = client.persistSampleResource(sampleTest1, true, true);
		if (!sampleTest1.equals(resource.getContent())) {
			log.warn("expected: "+sampleTest1);
			log.warn("found: "+resource.getContent());
			throw new RuntimeException("Expected response to equal submission");
		}
	}

	@Override
	protected void phaseTwo() {
	    // Test POSTing a sample with an accession to /samples should return 400 BAD REQUEST response
		this.postSampleWithAccessionShouldReturnABadRequestResponse();

		Sample sampleTest1 = getSampleTest1();
		// get to check it worked
		Optional<Resource<Sample>> optional = client.fetchSampleResource(sampleTest1.getAccession());
		if (!optional.isPresent()) {
			throw new RuntimeException("No existing "+sampleTest1.getAccession());
		}
		//check the update date
		if (Duration.between(sampleTest1.getUpdate(), optional.get().getContent().getUpdate())
				.abs().getSeconds() < 60) {
			throw new RuntimeException("Update date was not modified to within 60s as intended");			
		}
		//disabled because not fully operational
		//checkIfModifiedSince(optional.get());
		//checkIfMatch(optional.get());

		// put a version that is private
		sampleTest1 = Sample.build(sampleTest1.getName(), sampleTest1.getAccession(), sampleTest1.getDomain(),
				Instant.parse("2116-04-01T11:36:57.00Z"), sampleTest1.getUpdate(),
				sampleTest1.getCharacteristics(), sampleTest1.getRelationships(), sampleTest1.getExternalReferences(), null, null, null);
		Resource<Sample> resource = client.persistSampleResource(sampleTest1);
		if (!sampleTest1.equals(resource.getContent())) {
			log.warn("expected: "+sampleTest1);
			log.warn("found: "+resource.getContent());
			throw new RuntimeException("Expected response to equal submission");
		}

		//TODO check If-Unmodified-Since
		//TODO check If-None-Match
	}

	@Override
	protected void phaseThree() {
		Sample sampleTest1 = getSampleTest1();
		Sample sampleTest2 = getSampleTest2();
		Optional<Resource<Sample>> optional;
		
		//check that it is private 
		optional = annonymousClient.fetchSampleResource(sampleTest1.getAccession());
		if (optional.isPresent()) {
			throw new RuntimeException("Can access private "+sampleTest1.getAccession()+" as annonymous");
		}
				
		
		//check that it is accessible, if authorised
		optional = client.fetchSampleResource(sampleTest1.getAccession());
		if (!optional.isPresent()) {
			throw new RuntimeException("Cannot access private "+sampleTest1.getAccession());
		}
		
		//put the second sample in
		Resource<Sample> resource = client.persistSampleResource(sampleTest2, false, true);
		sampleTest2 = Sample.build(sampleTest2.getName(), sampleTest2.getAccession(), "self.BiosampleIntegrationTest",
				sampleTest2.getRelease(), sampleTest2.getUpdate(),
				sampleTest2.getCharacteristics(), sampleTest1.getRelationships(), sampleTest2.getExternalReferences(),
				null, null, null);

		if (!sampleTest2.equals(resource.getContent())) {
			log.warn("expected: "+sampleTest2);
			log.warn("found: "+resource.getContent());
			throw new RuntimeException("Expected response to equal submission");
		}
	}

	@Override
	protected void phaseFour() {
		Sample sampleTest1 = getSampleTest1();
		Sample sampleTest2 = getSampleTest2();
		//at this point, the inverse relationship should have been added
		
		sampleTest2 = Sample.build(sampleTest2.getName(), sampleTest2.getAccession(), sampleTest2.getDomain(),
				sampleTest2.getRelease(), sampleTest2.getUpdate(),
				sampleTest2.getCharacteristics(), sampleTest1.getRelationships(), sampleTest2.getExternalReferences(), null, null, null);

		//check that it has the additional relationship added
		// get to check it worked
		Optional<Resource<Sample>> optional = client.fetchSampleResource(sampleTest2.getAccession());
		if (!optional.isPresent()) {
			throw new RuntimeException("No existing "+sampleTest2.getAccession());
		}
		Sample sampleTest2Rest = optional.get().getContent();
		//check other details i.e relationship
		if (!sampleTest2.equals(sampleTest2Rest)) {
			log.warn("expected: "+sampleTest2);
			log.warn("found: "+sampleTest2Rest);
			throw new RuntimeException("No matching "+sampleTest2.getAccession());
		}
		//check utf -8
		if (!sampleTest2Rest.getCharacteristics().contains(Attribute.build("UTF-8 test", "αβ"))) {
			throw new RuntimeException("Unable to find UTF-8 characters");
		}
		//check the update date
		if (!sampleTest2Rest.getUpdate().equals(sampleTest2.getUpdate())) {
			log.info("sampleTest2Rest.getUpdate() = "+sampleTest2Rest.getUpdate());
			log.info("sampleTest2.getUpdate() = "+sampleTest2.getUpdate());
			throw new RuntimeException("Update date was modified when it shouldn't have been");			
		}
		//now do another update to delete the relationship
		sampleTest1 = Sample.build(sampleTest1.getName(), sampleTest1.getAccession(), sampleTest1.getDomain(),
				Instant.parse("2116-04-01T11:36:57.00Z"), sampleTest1.getUpdate(),
				sampleTest1.getCharacteristics(), new TreeSet<>(), sampleTest1.getExternalReferences(), null, null, null);
		Resource<Sample> resource = client.persistSampleResource(sampleTest1);
		if (!sampleTest1.equals(resource.getContent())) {
			log.warn("expected: "+sampleTest1);
			log.warn("found: "+resource.getContent());
			throw new RuntimeException("Expected response to equal submission");
		}
		
	}
	
	@Override
	protected void phaseFive() {	
		//check that deleting the relationship actually deleted it
		Sample sampleTest2 = getSampleTest2();
		Optional<Resource<Sample>> optional = client.fetchSampleResource(sampleTest2.getAccession());
		if (!optional.isPresent()) {
			throw new RuntimeException("No existing "+sampleTest2.getAccession());
		}
		Sample sampleTest2Rest = optional.get().getContent();
		//check other details i.e relationship
		if (!sampleTest2.equals(sampleTest2Rest)) {
			log.warn("expected: "+sampleTest2);
			log.warn("found: "+sampleTest2Rest);
			throw new RuntimeException("No matching "+sampleTest2.getAccession());
		}
		
	}

	private void checkIfModifiedSince(Resource<Sample> sample) {
		HttpHeaders headers = new HttpHeaders();
		headers.setIfModifiedSince(0);
		ResponseEntity<Resource<Sample>> response = restTemplate.exchange(sample.getLink(Link.REL_SELF).getHref(), 
				HttpMethod.GET, new HttpEntity<Void>(headers), 
				new ParameterizedTypeReference<Resource<Sample>>(){});
		
		if (!response.getStatusCode().equals(HttpStatus.NOT_MODIFIED)) {
			throw new RuntimeException("Got something other than a 304 response");
		}
	}
	
	private void checkIfMatch(Resource<Sample> sample) {
		HttpHeaders headers = new HttpHeaders();
		headers.setIfNoneMatch("W/\""+sample.getContent().hashCode()+"\"");
		ResponseEntity<Resource<Sample>> response = restTemplate.exchange(sample.getLink(Link.REL_SELF).getHref(), 
				HttpMethod.GET, new HttpEntity<Void>(headers), 
				new ParameterizedTypeReference<Resource<Sample>>(){});
		
		if (!response.getStatusCode().equals(HttpStatus.NOT_MODIFIED)) {
			throw new RuntimeException("Got something other than a 304 response");
		}
	}
	
	
	private Sample getSampleTest1() {
		String name = "Test Sample";
		String accession = "TESTrest1";
        String domain = "self.BiosampleIntegrationTest";
		Instant update = Instant.parse("2016-05-05T11:36:57.00Z");
		Instant release = Instant.parse("2016-04-01T11:36:57.00Z");

		SortedSet<Attribute> attributes = new TreeSet<>();
		attributes.add(
				Attribute.build("organism", "Homo sapiens", "http://purl.obolibrary.org/obo/NCBITaxon_9606", null));
		attributes.add(Attribute.build("age", "3", Collections.emptyList(), "year"));
		attributes.add(Attribute.build("organism part", "lung"));
		attributes.add(Attribute.build("organism part", "heart"));
		attributes.add(Attribute.build("sex", "female", Sets.newHashSet("http://purl.obolibrary.org/obo/PATO_0000383","http://www.ebi.ac.uk/efo/EFO_0001265"), null));

		SortedSet<Relationship> relationships = new TreeSet<>();
		relationships.add(Relationship.build("TESTrest1", "derived from", "TESTrest2"));
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
//				.firstName("Jo")
//				.lastName("Bloggs")
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

		return Sample.build(name, accession, domain, release, update, attributes, relationships, externalReferences, organizations, contacts, publications);
	}
	
	@PreDestroy
	public void destroy() {
		annonymousClient.close();
	}

	private Sample getSampleTest2() {
		String name = "Test Sample the second";
		String accession = "TESTrest2";
        String domain = "self.BiosampleIntegrationTest";
		Instant update = Instant.parse("2016-05-05T11:36:57.00Z");
		Instant release = Instant.parse("2016-04-01T11:36:57.00Z");

		SortedSet<Attribute> attributes = new TreeSet<>();
		attributes.add(
				Attribute.build("organism", "Homo sapiens", "http://purl.obolibrary.org/obo/NCBITaxon_9606", null));
		attributes.add(Attribute.build("UTF-8 test", "αβ"));

		return Sample.build(name, accession, domain, release, update, attributes, new TreeSet<>(), new TreeSet<>(), null, null, null);
	}

	private void postSampleWithAccessionShouldReturnABadRequestResponse() {


		Traverson traverson = new Traverson(this.clientProperties.getBiosamplesClientUri(), MediaTypes.HAL_JSON);
		Traverson.TraversalBuilder builder = traverson.follow("samples");
		log.info("POSTing sample with accession from " + builder.asLink().getHref());

		MultiValueMap<String, String> sample= new LinkedMultiValueMap<String, String>();
		sample.add("name", "test_sample");
		sample.add("accession", "SAMEA09123842");
		sample.add("domain", "self.BiosampleIntegrationTest");
		sample.add("release", "2016-05-05T11:36:57.00Z");
		sample.add("update", "2016-04-01T11:36:57.00Z");

		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
		httpHeaders.setContentType(MediaType.APPLICATION_JSON);

		HttpEntity<MultiValueMap> entity = new HttpEntity<>(sample, httpHeaders);

		try {
			restTemplate.exchange(
					builder.asLink().getHref(),
					HttpMethod.POST,
					entity,
					String.class);
		} catch (HttpStatusCodeException sce) {
			if ( ! sce.getStatusCode().equals(HttpStatus.BAD_REQUEST) ) {
				throw new RuntimeException("POSTing to samples endpoint a sample with an accession should return a 400 Bad Request exception");
			}
		}

		log.info(String.format(
				"POSTing sample with accession from %s produced a BAD REQUEST as expected and wanted ",
				builder.asLink().getHref()
		));


	}

}
