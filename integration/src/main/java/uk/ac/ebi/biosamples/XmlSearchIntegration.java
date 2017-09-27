package uk.ac.ebi.biosamples;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Profile;
import org.springframework.hateoas.Resource;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.Sample;

import java.time.Instant;
import java.util.Collections;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;

@Component
@Profile({"default"})
public class XmlSearchIntegration extends AbstractIntegration {
    
    private final RestTemplate restTemplate;
    private final IntegrationProperties integrationProperties;

    Logger log = LoggerFactory.getLogger(getClass());

    public XmlSearchIntegration(BioSamplesClient client,
                                RestTemplateBuilder restTemplateBuilder,
                                IntegrationProperties integrationProperties) {
        super(client);
        this.restTemplate = restTemplateBuilder.build();
        this.integrationProperties = integrationProperties;

    }

    @Override
    protected void phaseOne() {
        final Sample test1 = getSampleXMLTest1();
        final Sample test2 = getPrivateSampleXMLTest2();

        Optional<Resource<Sample>> optional = client.fetchSampleResource(test1.getAccession());
        if (optional.isPresent()) {
            throw new RuntimeException("Found existing "+test1.getAccession());
        }

        Resource<Sample> resource = client.persistSampleResource(test1);
        if (!test1.equals(resource.getContent())) {
            throw new RuntimeException("Expected response to equal submission");
        }

        Optional<Resource<Sample>> optionalPrivate = client.fetchSampleResource(test2.getAccession());
        if (optionalPrivate.isPresent()) {
            throw new RuntimeException("Found existing "+test1.getAccession());
        }

        Resource<Sample> resourcePrivate = client.persistSampleResource(test2);
        if (!test2.equals(resourcePrivate.getContent())) {
            throw new RuntimeException("Expected response to equal submission");
        }
    }

    @Override
    protected void phaseTwo() {
        Sample test1 = getSampleXMLTest1();
        Sample test2 = getPrivateSampleXMLTest2();

        log.info("Check existence of sample " + test1.getAccession());
        Optional<Resource<Sample>> optional = client.fetchSampleResource(test1.getAccession());
        if (!optional.isPresent()) {
            throw new RuntimeException("Expected sample not found "+test1.getAccession());
        }
        log.info("Sample " + test1.getAccession() + " found correctly");


        log.info(String.format("Searching sample %s using legacy xml api", test1.getAccession()));
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUri(integrationProperties.getBiosampleLegaxyXmlUri());
        uriBuilder.pathSegment("samples", test1.getAccession());

        RequestEntity<?> request = RequestEntity.get(uriBuilder.build().toUri()).accept(MediaType.TEXT_XML).build();
        ResponseEntity<String> responseEntity = restTemplate.exchange(request, String.class);       
        
        log.info(String.format("Sample %s found using legacy xml api", test1.getAccession()));

        if (!responseEntity.getBody().contains(String.format("id=\"%s\"",test1.getAccession()))) {
            throw new RuntimeException("Response body doesn't match expected sample");
        }

        assert responseEntity.getBody().equals(test1);

        log.info(String.format("Searching private sample %s using legacy xml api", test1.getAccession()));
        uriBuilder = UriComponentsBuilder.fromUri(integrationProperties.getBiosampleLegaxyXmlUri());
        uriBuilder.pathSegment("samples", test2.getAccession());

        try {
            RequestEntity<?> sampleRequestEntity = RequestEntity.get(uriBuilder.build().toUri()).accept(MediaType.TEXT_XML).build();
            ResponseEntity<Sample> sampleResponseEntity = restTemplate.exchange(sampleRequestEntity, Sample.class);
            if (sampleResponseEntity.getStatusCode().equals(HttpStatus.OK)) {
                throw new RuntimeException(String.format("Sample %s should be not available through the legaxy xml api", test2.getAccession()));
            }
        } catch (HttpClientErrorException e) {
            log.info("e.getStatusCode() = "+e.getStatusCode());
            if (e.getStatusCode().equals(HttpStatus.OK)) {
                throw new RuntimeException(String.format("Sample %s should be not available through the legaxy xml api", test2.getAccession()));
            }
        }

    }

    @Override
    protected void phaseThree() {

        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUri(integrationProperties.getBiosampleLegaxyXmlUri());

        UriComponentsBuilder testBadRequest= uriBuilder.cloneBuilder();
        testBadRequest.pathSegment("samples");

        // Accept text/xml header
        HttpHeaders xmlHeaders = new HttpHeaders();
        xmlHeaders.setAccept(Collections.singletonList(MediaType.TEXT_XML));

        // Accept application/json header
        HttpHeaders jsonHeaders = new HttpHeaders();
        jsonHeaders.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        log.info("Try to generate a BAD REQUEST using legacy xml samples end-point without required parameter");
        try {
            restTemplate.exchange(testBadRequest.toUriString(),
                    HttpMethod.GET,
                    new HttpEntity<>(xmlHeaders),
                    Sample.class);
        } catch(HttpClientErrorException ex) {
            boolean expectedResponse = ex.getStatusCode().is4xxClientError();

            if (!expectedResponse) {
                throw new RuntimeException("Excepted response doesn't match 4xx client error", ex);
            }
            expectedResponse = expectedResponse && ex.getRawStatusCode() == 400;
            if (!expectedResponse) {
                throw new RuntimeException("Excepted response doesn't match 400 BAD REQUEST", ex);
            }
            expectedResponse = expectedResponse && ex.getResponseHeaders().getContentType().includes(MediaType.TEXT_XML);
            if (!expectedResponse) {
                throw new RuntimeException("Excepted response content-type doesn't match text/xml", ex);
            }

        }

        // Check application/json request
        log.info("Try to generate a NOT ACCEPTABLE error using legacy xml samples end-point with application/json accept header");
        try {
            restTemplate.exchange(testBadRequest.toUriString(),
                    HttpMethod.GET,
                    new HttpEntity<>(jsonHeaders),
                    Sample.class);
        } catch(HttpClientErrorException ex) {
            boolean expectedResponse = ex.getStatusCode().is4xxClientError();
            if (!expectedResponse) {
                throw new RuntimeException("Excepted response doesn't match 4xx client error", ex);
            }
            expectedResponse = expectedResponse && ex.getRawStatusCode() == 406;
            if (!expectedResponse) {
                throw new RuntimeException("Excepted response doesn't match 406 NOT ACCEPTABLE", ex);
            }
            expectedResponse = expectedResponse && ex.getResponseHeaders().getContentType().includes(MediaType.APPLICATION_JSON);
            if (!expectedResponse) {
                throw new RuntimeException("Excepted response content-type doesn't match application/json", ex);
            }
        }

        UriComponentsBuilder testProperRequest= uriBuilder.cloneBuilder();
        testProperRequest.pathSegment("samples");
        testProperRequest.queryParam("query", "test");


        log.info("Try to retrieve a result query object from the legacy xml api");
        ResponseEntity<String> responseEntity = restTemplate.exchange(
                    testProperRequest.toUriString(),
                    HttpMethod.GET,
                    new HttpEntity<>(xmlHeaders),
                    String.class);
        if (!responseEntity.getStatusCode().is2xxSuccessful() ||
                responseEntity.getBody() == null) {
            throw new RuntimeException("Unexpected result for pagination query in the legacy xml");
        }

    }

    @Override
    protected void phaseFour() {

    }

    @Override
    protected void phaseFive() {

    }

    private Sample getSampleXMLTest1() {
		String name = "Test XML Sample";
		String accession = "SAMEAXML123123";
		Instant update = Instant.parse("2016-05-05T11:36:57.00Z");
		Instant release = Instant.parse("2016-04-01T11:36:57.00Z");

		SortedSet<Attribute> attributes = new TreeSet<>();
		attributes.add(
			Attribute.build("organism", "Homo sapiens", "http://purl.obolibrary.org/obo/NCBITaxon_9606", null));
		return Sample.build(name, accession, "self.BiosampleIntegrationTest", release, update, attributes, new TreeSet<>(), new TreeSet<>());
	}

	private Sample getPrivateSampleXMLTest2() {
        String name = "Private XML sample";
        String accession = "TestPrivateXML";
		Instant update = Instant.parse("2016-05-05T11:36:57.00Z");
		Instant release = Instant.parse("2116-04-01T11:36:57.00Z");

        SortedSet<Attribute> attributes = new TreeSet<>();
        attributes.add(
                Attribute.build("organism", "Homo sapiens", "http://purl.obolibrary.org/obo/NCBITaxon_9606", null));

        return Sample.build(name, accession, "self.BiosampleIntegrationTest", release, update, attributes, new TreeSet<>(), new TreeSet<>());

    }
}
