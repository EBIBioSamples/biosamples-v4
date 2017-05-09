package uk.ac.ebi.biosamples;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;
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
import uk.ac.ebi.biosamples.model.Curation;
import uk.ac.ebi.biosamples.model.ExternalReference;
import uk.ac.ebi.biosamples.model.ExternalReferenceLink;
import uk.ac.ebi.biosamples.model.Relationship;
import uk.ac.ebi.biosamples.model.Sample;

@Component
@Order(6)
@Profile({ "default", "rest" })
public class RestCurationRunner implements ApplicationRunner, ExitCodeGenerator {

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

		log.info("Starting RestCurationRunner");
		Sample sample = getSampleTest1();

		if (args.containsOption("phase") && Integer.parseInt(args.getOptionValues("phase").get(0)) == 1) {

			client.persistSample(sample);
			
		} else if (args.containsOption("phase") && Integer.parseInt(args.getOptionValues("phase").get(0)) == 2) {

			Set<Attribute> attributesPre = new HashSet<>();
			attributesPre.add(Attribute.build("Organism", "9606"));
			Set<Attribute> attributesPost = new HashSet<>();
			attributesPost.add(Attribute.build("Organism", "Homo sapiens"));			
			client.persistCuration(sample.getAccession(), Curation.build(attributesPre, attributesPost));


			attributesPre = new HashSet<>();
			attributesPre.add(Attribute.build("Organism", "Homo sapiens"));
			attributesPost = new HashSet<>();
			attributesPost.add(Attribute.build("Organism", "Homo sapiens", "http://purl.obolibrary.org/obo/NCBITaxon_9606", null));			
			client.persistCuration(sample.getAccession(), Curation.build(attributesPre, attributesPost));

		} else if (args.containsOption("phase") && Integer.parseInt(args.getOptionValues("phase").get(0)) == 3) {
			
			// check /curations
			testCurations();
			
			testSampleCurations(sample);
		}

		// if we got here without throwing, then we finished successfully
		exitCode = 0;
		log.info("Finished RestCurationRunner");
	}

	private void testCurations() {
		//TODO use client
		URI uri = UriComponentsBuilder.fromUri(integrationProperties.getBiosampleSubmissionUri())
				.pathSegment("curations").build().toUri();

		log.info("GETting from " + uri);
		RequestEntity<Void> request = RequestEntity.get(uri).accept(MediaTypes.HAL_JSON).build();
		ResponseEntity<PagedResources<Resource<Curation>>> response = restTemplate.exchange(request,
				new ParameterizedTypeReference<PagedResources<Resource<Curation>>>() {
				});

		boolean testedSelf = false;
		PagedResources<Resource<Curation>> paged = response.getBody();

		for (Resource<Curation> curationResource : paged) {
			Link selfLink = curationResource.getLink("self");

			if (selfLink == null) {
				throw new RuntimeException("Must have self link on "+curationResource);
			}

			if (curationResource.getLink("samples") == null) {
				throw new RuntimeException("Must have samples link on "+curationResource);
			}

			if (!testedSelf) {
				URI uriLink = URI.create(selfLink.getHref());
				log.info("GETting from " + uriLink);
				RequestEntity<Void> requestLink = RequestEntity.get(uriLink).accept(MediaTypes.HAL_JSON).build();
				ResponseEntity<Resource<Curation>> responseLink = restTemplate.exchange(requestLink,
						new ParameterizedTypeReference<Resource<Curation>>() {
						});
				if (!responseLink.getStatusCode().is2xxSuccessful()) {
					throw new RuntimeException("Unable to follow self link on "+curationResource);
				}
				testedSelf = true;
			}
		}

	}

	private void testSampleCurations(Sample sample) {
		//TODO use client
		URI uri = UriComponentsBuilder.fromUri(integrationProperties.getBiosampleSubmissionUri()).pathSegment("samples")
				.pathSegment(sample.getAccession()).pathSegment("curationlinks").build().toUri();

		log.info("GETting from " + uri);
		RequestEntity<Void> request = RequestEntity.get(uri).accept(MediaTypes.HAL_JSON).build();
		ResponseEntity<PagedResources<Resource<Curation>>> response = restTemplate.exchange(request,
				new ParameterizedTypeReference<PagedResources<Resource<Curation>>>() {
				});

		PagedResources<Resource<Curation>> paged = response.getBody();

		if (paged.getMetadata().getTotalElements() != 2) {
			throw new RuntimeException(
					"Expecting 2 external references, found " + paged.getMetadata().getTotalElements());
		}

	}

	private Sample getSampleTest1() throws URISyntaxException {
		String name = "Test Sample";
		String accession = "TESTCur1";
		LocalDateTime update = LocalDateTime.of(LocalDate.of(2016, 5, 5), LocalTime.of(11, 36, 57, 0));
		LocalDateTime release = LocalDateTime.of(LocalDate.of(2016, 4, 1), LocalTime.of(11, 36, 57, 0));

		SortedSet<Attribute> attributes = new TreeSet<>();
		attributes.add(Attribute.build("Organism", "9606"));

		SortedSet<Relationship> relationships = new TreeSet<>();

		SortedSet<ExternalReference> externalReferences = new TreeSet<>();

		return Sample.build(name, accession, release, update, attributes, relationships, externalReferences);
	}

	@Override
	public int getExitCode() {
		return exitCode;
	}

}
