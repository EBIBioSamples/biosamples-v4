package uk.ac.ebi.biosamples;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Profile;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.annotation.Order;
import org.springframework.hateoas.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.SampleFacet;

import java.net.URI;
import java.time.LocalDate;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

@Component
@Order(3)
@Profile({"default", "test"})
public class RestFacetIntegration extends AbstractIntegration {

	private Logger log = LoggerFactory.getLogger(this.getClass());
	
	private final RestTemplate restTemplate;
	private final IntegrationProperties integrationProperties;
	
	public RestFacetIntegration(BioSamplesClient client, IntegrationProperties integrationProperties, RestTemplateBuilder restTemplateBuilder) {
		super(client);
		this.integrationProperties = integrationProperties;
		this.restTemplate = restTemplateBuilder.build();
	}

	@Override
	protected void phaseOne() {
		Sample sampleTest1 = getSampleTest1();		
		// put a sample
		Resource<Sample> resource = client.persistSampleResource(sampleTest1);
		if (!sampleTest1.equals(resource.getContent())) {
			throw new RuntimeException("Expected response to equal submission");
		}
	}

	@Override
	protected void phaseTwo() {

		URI uri = UriComponentsBuilder.fromUri(integrationProperties.getBiosampleSubmissionUri())
				.pathSegment("samples").pathSegment("facets").queryParam("text", "TESTrestfacet1").build().toUri();

		log.info("GETting from "+uri);
		RequestEntity<Void> request = RequestEntity.get(uri).accept(MediaType.APPLICATION_JSON).build();
		ResponseEntity<List<SampleFacet>> response = restTemplate.exchange(request, new ParameterizedTypeReference<List<SampleFacet>>(){});
		//check that there is at least one sample returned
		//if there are zero, then probably nothing was indexed
		if (response.getBody().size() <= 0) {
			throw new RuntimeException("No facets found!");
		}
		if (response.getBody().get(0).size() <= 0) {
			throw new RuntimeException("No facet values found!");
		}
		
		//check that the particular facets we expect are present
		boolean found = false;
		for (SampleFacet facet : response.getBody()) {
			if (facet.getLabel().equals("geographic location (country and/or sea)")) {
				found = true;
				//check that it has one value that is expected
				if (facet.getValues().size() != 1) {
					throw new RuntimeException("More than one facet value for \"geographic location (country and/or sea)\"");
				}
				if (!facet.getValues().iterator().next().label.equals("Land of Oz")) {
					throw new RuntimeException("Facet value for \"geographic location (country and/or sea)\" was not \"Land of Oz\"");
				}
			}
		} 
		if (!found) {
			throw new RuntimeException("Unable to find facet \"geographic location (country and/or sea)\"");
		}
		
	}

	@Override
	protected void phaseThree() {
	}

	@Override
	protected void phaseFour() {
	}

	@Override
	protected void phaseFive() {
	}

	private Sample getSampleTest1() {
		String name = "Test Sample";
		String accession = "TESTrestfacet1";
		String domain = "self.BiosampleIntegrationTest";
		Instant update = Instant.parse("2016-05-05T11:36:57.00Z");
		Instant release = Instant.parse("2016-04-01T11:36:57.00Z");

		SortedSet<Attribute> attributes = new TreeSet<>();
		attributes.add(
			Attribute.build("organism", "Homo sapiens", "http://purl.obolibrary.org/obo/NCBITaxon_9606", null));
		//use non alphanumeric characters in type
		attributes.add(Attribute.build("geographic location (country and/or sea)", "Land of Oz", null, null));

		return Sample.build(name, accession, domain, release, update, attributes, null, null);
	}
	
}
