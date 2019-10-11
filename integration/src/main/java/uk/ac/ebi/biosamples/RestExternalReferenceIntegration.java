package uk.ac.ebi.biosamples;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.*;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;

@Component
@Order(6)
//@Profile({ "default", "rest" })
public class RestExternalReferenceIntegration extends AbstractIntegration {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	public RestExternalReferenceIntegration(BioSamplesClient client) {
		super(client);
	}

	@Override
	protected void phaseOne() {
		Sample sample = getSampleTest1();
		client.persistSample(sample);
	}

	@Override
	protected void phaseTwo() {
		Sample sample = getSampleTest1();

		testExternalReferences();
		//testSampleExternalReferences(sample, 10);
		client.persistCuration(sample.getAccession(),
				Curation.build(null,  null, null, Arrays.asList(ExternalReference.build("http://www.ebi.ac.uk/ena/ERA123456"))),
				"self.BiosampleIntegrationTest");

	}

	@Override
	protected void phaseThree() {
		Sample sample = getSampleTest1();
		//testSampleExternalReferences(sample, 11);
		//check there was no side-effects
		client.fetchSample(sample.getAccession());
	}

	@Override
	protected void phaseFour() {

	}

	@Override
	protected void phaseFive() {

	}
	private void testExternalReferences() {
/*
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
*/
	}

	private void testSampleExternalReferences(Sample sample, int expectedCount) {
		sample = client.fetchSample(sample.getAccession()).get();


		if (sample.getExternalReferences().size() != expectedCount) {
			throw new RuntimeException("Expecting " + expectedCount + " external references, found "
					+ sample.getExternalReferences().size());
		}

	}

	private Sample getSampleTest1() {
		String name = "Test Sample";
		String accession = "TESTExRef1";
        String domain = "self.BiosampleIntegrationTest";
		Instant update = Instant.parse("2016-05-05T11:36:57.00Z");
		Instant release = Instant.parse("2016-04-01T11:36:57.00Z");

		SortedSet<Attribute> attributes = new TreeSet<>();

		SortedSet<Relationship> relationships = new TreeSet<>();

		SortedSet<ExternalReference> externalReferences = new TreeSet<>();
		externalReferences.add(ExternalReference.build("http://www.ega-archive.org/datasets/1", new TreeSet<>(Collections.singleton("DUO:0000007"))));
		externalReferences.add(ExternalReference.build("http://www.ega-archive.org/metadata/2",
				new TreeSet<>(Arrays.asList("DUO:0000005", "DUO:0000001", "DUO:0000007"))));
		externalReferences.add(ExternalReference.build("http://www.hpscreg.eu/3"));
		externalReferences.add(ExternalReference.build("http://www.test.com/4"));
		externalReferences.add(ExternalReference.build("http://www.ebi.ac.uk/arrayexpress/5"));
		externalReferences.add(ExternalReference.build("http://www.test.com/6"));
		externalReferences.add(ExternalReference.build("http://www.ebi.ac.uk/biostudies/7"));
		externalReferences.add(ExternalReference.build("http://www.ega-archive.org/datasets/EGAD00001001600",
				new TreeSet<>(Arrays.asList("DUO:0000005", "DUO:0000014", "DUO:0000019", "DUO:0000026", "DUO:0000028"))));

		return new Sample.Builder(name, accession).withDomain(domain)
				.withRelease(release).withUpdate(update)
				.withAttributes(attributes).withRelationships(relationships).withExternalReferences(externalReferences)
				.build();
	}

}
