package uk.ac.ebi.biosamples;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.SortedSet;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.core.Ordered;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.annotation.Order;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestOperations;
import org.springframework.web.util.UriComponentsBuilder;

import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.Relationship;
import uk.ac.ebi.biosamples.model.Sample;

@Component
public class RestRunner implements ApplicationRunner, ExitCodeGenerator, Ordered {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private IntegrationProperties integrationProperties;

	@Autowired
	private RestOperations restTemplate;
	
	private int exitCode = 1;

	@Override
	public void run(ApplicationArguments args) throws Exception {

		log.info("Starting RestRunner");
		
		Sample sampleTest1 = getSampleTest1();
		Sample sampleTest2 = getSampleTest2();
	
		if (args.containsOption("phase") && Integer.parseInt(args.getOptionValues("phase").get(0)) == 1) {
	
			// get and check that nothing exists already
			doGetAndFail(sampleTest1);
	
			// put a sample
			doPut(sampleTest1);
	
			// get to check it worked
			doGetAndSucess(sampleTest1);
	
			// put a version that is private
			sampleTest1 = Sample.build(sampleTest1.getName(), sampleTest1.getAccession(),
					LocalDateTime.of(LocalDate.of(2116, 4, 1), LocalTime.of(11, 36, 57, 0)), sampleTest1.getUpdate(),
					sampleTest1.getAttributes(), sampleTest1.getRelationships(), sampleTest1.getExternalReferences());
			doPut(sampleTest1);
	
			// check the response code
			doGetAndPrivate(sampleTest1);
			
			//put the second sample in
			doPut(sampleTest2);
			
			// get to check it worked
			doGetAndSucess(sampleTest2);
			
		} else if (args.containsOption("phase") && Integer.parseInt(args.getOptionValues("phase").get(0)) == 2) {
			
			//at this point, the agents should have run and updated things

			sampleTest2 = Sample.build(sampleTest2.getName(), sampleTest2.getAccession(),
					sampleTest2.getRelease(), sampleTest2.getUpdate(),
					sampleTest2.getAttributes(), sampleTest1.getRelationships(), new TreeSet<>());
			
			//check that it has the additional relationship added
			// get to check it worked
			doGetAndSucess(sampleTest2);
			
			//now do another update to delete the relationship
			//might as well make it public now too
			sampleTest1 = Sample.build(sampleTest1.getName(), sampleTest1.getAccession(),
					LocalDateTime.of(LocalDate.of(2016, 4, 1), LocalTime.of(11, 36, 57, 0)), sampleTest1.getUpdate(),
					sampleTest1.getAttributes(), new TreeSet<>(), sampleTest1.getExternalReferences());
			doPut(sampleTest1);
		}
		
		//TODO check that deleting a relationships on an update actually deletes it from get too
		
		//if we got here without throwing, then we finished sucessfully
		exitCode = 0;
		
		log.info("Finished RestRunner");
	}

	public Resource<Sample> doPut(Sample sample) throws RestClientException {
		URI uri = UriComponentsBuilder.fromUri(integrationProperties.getBiosampleSubmissionUri()).pathSegment("samples")
				.pathSegment(sample.getAccession()).build().toUri();

		log.info("PUTting to "+uri);
		RequestEntity<Sample> request = RequestEntity.put(uri).contentType(MediaType.APPLICATION_JSON).body(sample);
		ResponseEntity<Resource<Sample>> response = restTemplate.exchange(request, new ParameterizedTypeReference<Resource<Sample>>(){});
		if (!sample.equals(response.getBody().getContent())) {
			log.info("sample = "+sample);
			log.info("response.getBody() = "+response.getBody());
			throw new RuntimeException("Expected response to equal submission");
		}
		return response.getBody();
	}

	public Sample doGetAndSucess(Sample sample) {
		ResponseEntity<Resource<Sample>> response = doGet(sample);
		// check the status code is 200 success
		if (!HttpStatus.OK.equals(response.getStatusCode())) {
			throw new RuntimeException("Expected a 200 response");
		}
		if (!sample.equals(response.getBody().getContent())) {
			log.info("sample = "+sample);
			log.info("response.getBody() = "+response.getBody());
			throw new RuntimeException("Expected response to equal submission");
		}
		
		return response.getBody().getContent();
	}

	public void doGetAndPrivate(Sample sample) {
		try {
			doGet(sample);
		} catch (HttpStatusCodeException e) {
			if (HttpStatus.FORBIDDEN.equals(e.getStatusCode())) {
				// we expect to get a 403 error
				return;
			} else {
				// we got something else
				throw e;
			}
		}
		throw new RuntimeException("Expected a 403 response");
	}

	public void doGetAndFail(Sample sample) {
		try {
			doGet(sample);
		} catch (HttpStatusCodeException e) {
			if (HttpStatus.NOT_FOUND.equals(e.getStatusCode())) {
				// we expect to get a 404 error
				return;
			} else {
				// we got something else
				throw e;
			}
		}
		throw new RuntimeException("Expected a 404 response");
	}

	public ResponseEntity<Resource<Sample>> doGet(Sample sample) throws RestClientException {
		URI uri = UriComponentsBuilder.fromUri(integrationProperties.getBiosampleSubmissionUri()).pathSegment("samples")
				.pathSegment(sample.getAccession()).build().toUri();

		log.info("GETting from "+uri);
		RequestEntity<Void> request = RequestEntity.get(uri).accept(MediaTypes.HAL_JSON).build();
		ResponseEntity<Resource<Sample>> response = restTemplate.exchange(request, new ParameterizedTypeReference<Resource<Sample>>(){});
		return response;
	}

	private Sample getSampleTest1() throws URISyntaxException {
		String name = "Test Sample";
		String accession = "TEST1";
		LocalDateTime update = LocalDateTime.of(LocalDate.of(2016, 5, 5), LocalTime.of(11, 36, 57, 0));
		LocalDateTime release = LocalDateTime.of(LocalDate.of(2016, 4, 1), LocalTime.of(11, 36, 57, 0));

		SortedSet<Attribute> attributes = new TreeSet<>();
		attributes.add(
			Attribute.build("organism", "Homo sapiens", new URI("http://purl.obolibrary.org/obo/NCBITaxon_9606"), null));
		attributes.add(Attribute.build("age", "3", null, "year"));
		attributes.add(Attribute.build("organism part", "lung", null, null));
		attributes.add(Attribute.build("organism part", "heart", null, null));

		SortedSet<Relationship> relationships = new TreeSet<>();
		relationships.add(Relationship.build("derived from", "TEST2", "TEST1"));
		
		SortedSet<URI> externalReferences = new TreeSet<>();
		externalReferences.add(URI.create("http://www.google.com"));

		return Sample.build(name, accession, release, update, attributes, relationships, externalReferences);
	}

	private Sample getSampleTest2() throws URISyntaxException {
		String name = "Test Sample the second";
		String accession = "TEST2";
		LocalDateTime update = LocalDateTime.of(LocalDate.of(2016, 5, 5), LocalTime.of(11, 36, 57, 0));
		LocalDateTime release = LocalDateTime.of(LocalDate.of(2016, 4, 1), LocalTime.of(11, 36, 57, 0));

		SortedSet<Attribute> attributes = new TreeSet<>();
		attributes.add(
			Attribute.build("organism", "Homo sapiens", new URI("http://purl.obolibrary.org/obo/NCBITaxon_9606"), null));

		return Sample.build(name, accession, release, update, attributes, new TreeSet<>(), new TreeSet<>());
	}

	@Override
	public int getOrder() {
		return 1;
	}

	@Override
	public int getExitCode() {
		return exitCode;
	}

}
