package uk.ac.ebi.biosamples;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.SortedSet;
import java.util.TreeSet;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
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
public class TestRunner implements ApplicationRunner {

	@Autowired
	private IntegrationProperties integrationProperties;

	@Autowired
	private RestOperations restTemplate;

	@Override
	public void run(ApplicationArguments args) throws Exception {

			Sample sampleTest1 = getSampleTest1();

			// get and check that nothing exists already
			doGetAndFail(sampleTest1);

			// put a sample
			doPut(sampleTest1);

			// get to check it worked
			doGetAndSucess(sampleTest1);

			// put a version that is private
			sampleTest1 = Sample.build(sampleTest1.getName(), sampleTest1.getAccession(),
					LocalDateTime.of(LocalDate.of(2116, 4, 1), LocalTime.of(11, 36, 57, 0)), sampleTest1.getUpdate(),
					sampleTest1.getAttributes(), sampleTest1.getRelationships());
			doPut(sampleTest1);

			// check the response code
			doGetAndPrivate(sampleTest1);
			
			//put the second sample in
			Sample sampleTest2 = getSampleTest2();
			doPut(sampleTest2);
			
			//check that it has the additional relationship added
			// get to check it worked
			Sample sampleTest2Get = doGetAndSucess(sampleTest2);
			if (sampleTest2Get.getRelationships() == null || sampleTest2Get.getRelationships().size() == 0) {
				throw new RuntimeException("No reverse relationship found");
			}
	}

	public ResponseEntity<Sample> doPut(Sample sample) throws RestClientException {
		URI uri = UriComponentsBuilder.fromUri(integrationProperties.getBiosampleSubmissionURI()).path("samples/")
				.path(sample.getAccession()).build().toUri();

		RequestEntity<Sample> request = RequestEntity.put(uri).contentType(MediaType.APPLICATION_JSON).body(sample);
		ResponseEntity<Sample> response = restTemplate.exchange(request, Sample.class);
		return response;
	}

	public Sample doGetAndSucess(Sample sample) {
		ResponseEntity<Sample> response = doGet(sample);
		// check the status code is 200 success
		if (!HttpStatus.OK.equals(response.getStatusCode())) {
			throw new RuntimeException("Expected a 200 response");
		}
		if (!sample.equals(response.getBody())) {
			throw new RuntimeException("Expected response to equal submission");
		}
		
		return response.getBody();
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

	public ResponseEntity<Sample> doGet(Sample sample) throws RestClientException {
		URI uri = UriComponentsBuilder.fromUri(integrationProperties.getBiosampleSubmissionURI()).path("samples/")
				.path(sample.getAccession()).build().toUri();

		RequestEntity<Void> request = RequestEntity.get(uri).accept(MediaTypes.HAL_JSON).build();
		ResponseEntity<Sample> response = restTemplate.exchange(request, Sample.class);
		return response;
	}

	private Sample getSampleTest1() throws URISyntaxException {
		String name = "Test Sample";
		String accession = "TEST1";
		LocalDateTime update = LocalDateTime.of(LocalDate.of(2016, 5, 5), LocalTime.of(11, 36, 57, 0));
		LocalDateTime release = LocalDateTime.of(LocalDate.of(2016, 4, 1), LocalTime.of(11, 36, 57, 0));

		SortedSet<Attribute> attributes = new TreeSet<>();
		attributes.add(
			Attribute.build("organism", "Homo sapiens", "http://purl.obolibrary.org/obo/NCBITaxon_9606", null));
		attributes.add(Attribute.build("age", "3", null, "year"));
		attributes.add(Attribute.build("organism part", "lung", null, null));
		attributes.add(Attribute.build("organism part", "heart", null, null));

		SortedSet<Relationship> relationships = new TreeSet<>();
		relationships.add(Relationship.build("derived from", "TEST2", "TEST1"));

		return Sample.build(name, accession, release, update, attributes, relationships);
	}

	private Sample getSampleTest2() throws URISyntaxException {
		String name = "Test Sample the second";
		String accession = "TEST2";
		LocalDateTime update = LocalDateTime.of(LocalDate.of(2016, 5, 5), LocalTime.of(11, 36, 57, 0));
		LocalDateTime release = LocalDateTime.of(LocalDate.of(2016, 4, 1), LocalTime.of(11, 36, 57, 0));

		SortedSet<Attribute> attributes = new TreeSet<>();
		attributes.add(
			Attribute.build("organism", "Homo sapiens", "http://purl.obolibrary.org/obo/NCBITaxon_9606", null));

		SortedSet<Relationship> relationships = new TreeSet<>();

		return Sample.build(name, accession, release, update, attributes, relationships);
	}

}
