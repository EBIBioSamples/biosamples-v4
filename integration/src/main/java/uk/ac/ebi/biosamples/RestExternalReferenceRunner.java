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
import org.springframework.context.annotation.Profile;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.annotation.Order;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestOperations;
import org.springframework.web.util.UriComponentsBuilder;

import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.ExternalReference;
import uk.ac.ebi.biosamples.model.Relationship;
import uk.ac.ebi.biosamples.model.Sample;

@Component
@Order(6)
@Profile({ "default", "rest" })
public class RestExternalReferenceRunner implements ApplicationRunner, ExitCodeGenerator {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private IntegrationProperties integrationProperties;

	@Autowired
	private RestOperations restTemplate;

	@Autowired
	private BioSamplesClient client;

	private int exitCode = 1;

	@Override
	public void run(ApplicationArguments args) throws Exception {

		log.info("Starting RestExternalReferenceRunner");
		Sample sample = getSampleTest1();

		if (args.containsOption("phase") && Integer.parseInt(args.getOptionValues("phase").get(0)) == 1) {

			client.persistSample(sample);

		} else if (args.containsOption("phase") && Integer.parseInt(args.getOptionValues("phase").get(0)) == 2) {
			// check /externalreferences
			testExternalReferences();

			testSampleExternalReferences(sample, 10);

			client.persistExternalReference(sample.getAccession(), "http://www.ebi.ac.uk/ena/ERA123456");
		} else if (args.containsOption("phase") && Integer.parseInt(args.getOptionValues("phase").get(0)) == 3) {

			testSampleExternalReferences(sample, 11);
		}

		// if we got here without throwing, then we finished successfully
		exitCode = 0;
		log.info("Finished RestSearchRunner");
	}

	private void testExternalReferences() {
		URI uri = UriComponentsBuilder.fromUri(integrationProperties.getBiosampleSubmissionUri())
				.pathSegment("externalreferences").build().toUri();

		log.info("GETting from " + uri);
		RequestEntity<Void> request = RequestEntity.get(uri).accept(MediaTypes.HAL_JSON).build();
		ResponseEntity<PagedResources<Resource<ExternalReference>>> response = restTemplate.exchange(request,
				new ParameterizedTypeReference<PagedResources<Resource<ExternalReference>>>() {
				});

		boolean testedSelf = false;
		PagedResources<Resource<ExternalReference>> paged = response.getBody();

		for (Resource<ExternalReference> externalReferenceResource : paged) {
			Link selfLink = externalReferenceResource.getLink("self");

			if (selfLink == null) {
				throw new RuntimeException("Must have self link on " + externalReferenceResource);
			}

			if (externalReferenceResource.getLink("samples") == null) {
				throw new RuntimeException("Must have samples link on " + externalReferenceResource);
			}

			if (!testedSelf) {
				URI uriLink = URI.create(selfLink.getHref());
				log.info("GETting from " + uriLink);
				RequestEntity<Void> requestLink = RequestEntity.get(uriLink).accept(MediaTypes.HAL_JSON).build();
				ResponseEntity<Resource<ExternalReference>> responseLink = restTemplate.exchange(requestLink,
						new ParameterizedTypeReference<Resource<ExternalReference>>() {
						});
				if (!responseLink.getStatusCode().is2xxSuccessful()) {
					throw new RuntimeException("Unable to follow self link on " + externalReferenceResource);
				}
				testedSelf = true;
			}
		}

	}

	private void testSampleExternalReferences(Sample sample, int expectedCount) {
		URI uri = UriComponentsBuilder.fromUri(integrationProperties.getBiosampleSubmissionUri()).pathSegment("samples")
				.pathSegment(sample.getAccession()).pathSegment("externalreferencelinks").build().toUri();

		log.info("GETting from " + uri);
		RequestEntity<Void> request = RequestEntity.get(uri).accept(MediaTypes.HAL_JSON).build();
		ResponseEntity<PagedResources<Resource<ExternalReference>>> response = restTemplate.exchange(request,
				new ParameterizedTypeReference<PagedResources<Resource<ExternalReference>>>() {
				});

		PagedResources<Resource<ExternalReference>> paged = response.getBody();

		if (paged.getMetadata().getTotalElements() != expectedCount) {
			throw new RuntimeException("Expecting " + expectedCount + " external references, found "
					+ paged.getMetadata().getTotalElements());
		}

	}

	private Sample getSampleTest1() throws URISyntaxException {
		String name = "Test Sample";
		String accession = "TESTExRef1";
		LocalDateTime update = LocalDateTime.of(LocalDate.of(2016, 5, 5), LocalTime.of(11, 36, 57, 0));
		LocalDateTime release = LocalDateTime.of(LocalDate.of(2016, 4, 1), LocalTime.of(11, 36, 57, 0));

		SortedSet<Attribute> attributes = new TreeSet<>();

		SortedSet<Relationship> relationships = new TreeSet<>();

		SortedSet<ExternalReference> externalReferences = new TreeSet<>();
		externalReferences.add(ExternalReference.build("http://www.test.com/1"));
		externalReferences.add(ExternalReference.build("http://www.test.com/2"));
		externalReferences.add(ExternalReference.build("http://www.test.com/3"));
		externalReferences.add(ExternalReference.build("http://www.test.com/4"));
		externalReferences.add(ExternalReference.build("http://www.test.com/5"));
		externalReferences.add(ExternalReference.build("http://www.test.com/6"));
		externalReferences.add(ExternalReference.build("http://www.test.com/7"));
		externalReferences.add(ExternalReference.build("http://www.test.com/8"));
		externalReferences.add(ExternalReference.build("http://www.test.com/9"));
		externalReferences.add(ExternalReference.build("http://www.test.com/0"));

		return Sample.build(name, accession, release, update, attributes, relationships, externalReferences);
	}

	@Override
	public int getExitCode() {
		return exitCode;
	}

}
