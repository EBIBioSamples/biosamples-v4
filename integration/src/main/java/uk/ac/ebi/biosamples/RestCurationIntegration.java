package uk.ac.ebi.biosamples;

import java.net.URI;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
	private final BioSamplesProperties bioSamplesProperties;
	private final RestOperations restTemplate;
	
	private final Sample sample = getSampleTest1();
	
	public RestCurationIntegration(RestTemplateBuilder restTemplateBuilder, 
			IntegrationProperties integrationProperties, 
			BioSamplesProperties bioSamplesProperties,
			BioSamplesClient client) {
		super(client);
		this.restTemplate = restTemplateBuilder.build();
		this.integrationProperties = integrationProperties;
		this.bioSamplesProperties = bioSamplesProperties;
	}

	@Override
	protected void phaseOne() {
		client.persistSample(sample);		
	}

	@Override
	protected void phaseTwo() {

		Set<Attribute> attributesPre = new HashSet<>();
		attributesPre.add(Attribute.build("Organism", "9606"));
		Set<Attribute> attributesPost = new HashSet<>();
		attributesPost.add(Attribute.build("Organism", "Homo sapiens"));			
		client.persistCuration(sample.getAccession(), Curation.build(attributesPre, attributesPost, null, null), "self.BiosampleIntegrationTest");


		attributesPre = new HashSet<>();
		attributesPre.add(Attribute.build("Organism", "Homo sapiens"));
		attributesPost = new HashSet<>();
		attributesPost.add(Attribute.build("Organism", "Homo sapiens", "http://purl.obolibrary.org/obo/NCBITaxon_9606", null));			
		client.persistCuration(sample.getAccession(), Curation.build(attributesPre, attributesPost, null, null), "self.BiosampleIntegrationTest");
		
	}

	@Override
	protected void phaseThree() {
		
		// check /curations
		testCurations();
		
		testSampleCurations(sample);
		
		//check there was no side-effects
		client.fetchSample(sample.getAccession());
		
	}

	@Override
	protected void phaseFour() {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void phaseFive() {
		// TODO Auto-generated method stub
		
	}

	private void testCurations() {
		/*
		//TODO use client
		URI uri = UriComponentsBuilder.fromUri(integrationProperties.getBiosampleSubmissionUri())
				.pathSegment("curations").build().toUri();
		
		log.info("GETting from " + uri);
		RequestEntity<Void> request = RequestEntity.get(uri).accept(MediaTypes.HAL_JSON).build();
		ResponseEntity<PagedResources<Resource<Curation>>> response = restTemplate.exchange(request,
				new ParameterizedTypeReference<PagedResources<Resource<Curation>>>() {
				});
		if(!response.getStatusCode().is2xxSuccessful()) {
			throw new RuntimeException("Unable to get curations list");
		}
		log.info("GETted from " + uri);
		PagedResources<Resource<Curation>> paged = response.getBody();

		
		if (paged.getValue().size() == 0) {
			throw new RuntimeException("No curations in list");
		}
		*/
		for (Resource<Curation> curationResource : client.fetchCurationResourceAll()) {
			Link selfLink = curationResource.getLink("self");
			Link samplesLink = curationResource.getLink("samples");

			if (selfLink == null) {
				throw new RuntimeException("Must have self link on "+curationResource);
			} else {
				URI uriLink = URI.create(selfLink.getHref());
				log.info("GETting from " + uriLink);
				RequestEntity<Void> requestLink = RequestEntity.get(uriLink).accept(MediaTypes.HAL_JSON).build();
				ResponseEntity<Resource<Curation>> responseLink = restTemplate.exchange(requestLink,
						new ParameterizedTypeReference<Resource<Curation>>() {
						});
				if (!responseLink.getStatusCode().is2xxSuccessful()) {
					throw new RuntimeException("Unable to follow self link on "+curationResource);
				}
				log.info("GETted from " + uriLink);
				
			}

			if (samplesLink == null) {
				throw new RuntimeException("Must have samples link on "+curationResource);
			} else {
				URI uriLink = URI.create(samplesLink.getHref());
				log.info("GETting from " + uriLink);
				RequestEntity<Void> requestLink = RequestEntity.get(uriLink).accept(MediaTypes.HAL_JSON).build();
				ResponseEntity<PagedResources<Resource<Sample>>> responseLink = restTemplate.exchange(requestLink,
						new ParameterizedTypeReference<PagedResources<Resource<Sample>>>() {
						});
				if (!responseLink.getStatusCode().is2xxSuccessful()) {
					throw new RuntimeException("Unable to follow samples link on "+curationResource);
				}				
				log.info("GETted from " + uriLink);
			}
		}
	}

	private void testSampleCurations(Sample sample) {
		//TODO use client
		URI uri = UriComponentsBuilder.fromUri(bioSamplesProperties.getBiosamplesClientUri()).pathSegment("samples")
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
        String domain = "self.BiosampleIntegrationTest";
		Instant update = Instant.parse("2016-05-05T11:36:57.00Z");
		Instant release = Instant.parse("2016-04-01T11:36:57.00Z");

		SortedSet<Attribute> attributes = new TreeSet<>();
		attributes.add(Attribute.build("Organism", "9606"));

		SortedSet<Relationship> relationships = new TreeSet<>();

		SortedSet<ExternalReference> externalReferences = new TreeSet<>();

		return Sample.build(name, accession, domain, release, update, attributes, relationships, externalReferences, null, null, null);
	}
}
