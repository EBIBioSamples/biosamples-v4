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
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.web.client.RestTemplateBuilder;
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
import uk.ac.ebi.biosamples.model.Relationship;
import uk.ac.ebi.biosamples.model.Sample;

@Component
@Order(6)
@Profile({ "default", "rest" })
public class RestCurationIntegration extends AbstractIntegration {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	private final IntegrationProperties integrationProperties;

	private final RestOperations restTemplate;
	
	public RestCurationIntegration(RestTemplateBuilder restTemplateBuilder, IntegrationProperties integrationProperties, BioSamplesClient client) {
		super(client);
		this.restTemplate = restTemplateBuilder.build();
		this.integrationProperties = integrationProperties;
	}

	@Override
	protected void phaseOne(){
		Sample sample = getSampleTest1();
		client.persistSample(getSampleTest1());
	}

	@Override
	protected void phaseTwo() {
		Sample sample = getSampleTest1();

		Set<Attribute> attributesPre = new HashSet<>();
		attributesPre.add(Attribute.build("Organism", "9606"));
		Set<Attribute> attributesPost = new HashSet<>();
		attributesPost.add(Attribute.build("Organism", "Homo sapiens"));			
		client.persistCuration(sample.getAccession(), Curation.build(attributesPre, attributesPost, null, null));


		attributesPre = new HashSet<>();
		attributesPre.add(Attribute.build("Organism", "Homo sapiens"));
		attributesPost = new HashSet<>();
		attributesPost.add(Attribute.build("Organism", "Homo sapiens", "http://purl.obolibrary.org/obo/NCBITaxon_9606", null));			
		client.persistCuration(sample.getAccession(), Curation.build(attributesPre, attributesPost, null, null));
		
	}

	@Override
	protected void phaseThree(){
		Sample sample = getSampleTest1();
		
		// check /curations
		testCurations();
		
		testSampleCurations(sample);
		
		//check there was no side-effects
		client.fetchSample(sample.getAccession());
		
	}

	@Override
	protected void phaseFour() {
		
	}

	@Override
	protected void phaseFive() {
		
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
					"Expecting 2 curations, found " + paged.getMetadata().getTotalElements());
		}

	}

	private Sample getSampleTest1() {
		String name = "Test Sample";
		String accession = "TESTCur1";
        String domain = null;// "abcde12345";
		LocalDateTime update = LocalDateTime.of(LocalDate.of(2016, 5, 5), LocalTime.of(11, 36, 57, 0));
		LocalDateTime release = LocalDateTime.of(LocalDate.of(2016, 4, 1), LocalTime.of(11, 36, 57, 0));

		SortedSet<Attribute> attributes = new TreeSet<>();
		attributes.add(Attribute.build("Organism", "9606"));

		SortedSet<Relationship> relationships = new TreeSet<>();

		SortedSet<ExternalReference> externalReferences = new TreeSet<>();

		return Sample.build(name, accession, domain, release, update, attributes, relationships, externalReferences);
	}

	@Override
	public int getExitCode() {
		return exitCode;
	}

}
