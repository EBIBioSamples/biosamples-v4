package uk.ac.ebi.biosamples;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
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
import uk.ac.ebi.biosamples.model.Relationship;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.legacyxml.BioSample;
import uk.ac.ebi.biosamples.model.legacyxml.ResultQuery;
import uk.ac.ebi.biosamples.utils.XmlPathBuilder;

import java.io.StringReader;
import java.time.Instant;
import java.util.Collections;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;

@Component
@Profile({"default", "test"})
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
        final Sample sampleGroup = getSampleGroup();
        final Sample sampleWithinGroup = getSampleWithinGroup();

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

        Resource<Sample> groupResource = client.persistSampleResource(sampleGroup);
        if (!sampleGroup.equals(groupResource.getContent())) {
            throw new RuntimeException("Expected response to equal submission");
        }


        Resource<Sample> sampleWithinGroupResource = client.persistSampleResource(sampleWithinGroup);
        // The result and the submitted will not be equal because of the new inverse relation created automatically
        if (!sampleWithinGroup.getAccession().equals(sampleWithinGroupResource.getContent().getAccession())) {
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

//        log.info("Try to generate a BAD REQUEST using legacy xml samples end-point without required parameter");
//        try {
//            restTemplate.exchange(testBadRequest.toUriString(),
//                    HttpMethod.GET,
//                    new HttpEntity<>(xmlHeaders),
//                    Sample.class);
//        } catch(HttpClientErrorException ex) {
//            boolean expectedResponse = ex.getStatusCode().is4xxClientError();
//
//            if (!expectedResponse) {
//                throw new RuntimeException("Excepted response doesn't match 4xx client error", ex);
//            }
//            expectedResponse = expectedResponse && ex.getRawStatusCode() == 400;
//            if (!expectedResponse) {
//                throw new RuntimeException("Excepted response doesn't match 400 BAD REQUEST", ex);
//            }
//            expectedResponse = expectedResponse && ex.getResponseHeaders().getContentType().includes(MediaType.TEXT_XML);
//            if (!expectedResponse) {
//                throw new RuntimeException("Excepted response content-type doesn't match text/xml", ex);
//            }
//
//        }

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
        // Search for sample and group
        Sample sampleGroup = getSampleGroup();
        Sample sampleWithinGroup = getSampleWithinGroup();

        HttpHeaders xmlHeaders = new HttpHeaders();
        xmlHeaders.setAccept(Collections.singletonList(MediaType.TEXT_XML));

        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUri(integrationProperties.getBiosampleLegaxyXmlUri());

        UriComponentsBuilder testGroupsEndpoint = uriBuilder.cloneBuilder();
        testGroupsEndpoint.pathSegment("groups", sampleGroup.getAccession());


        ResponseEntity<String> response = restTemplate.exchange(testGroupsEndpoint.toUriString(),
                HttpMethod.GET,
                new HttpEntity<>(xmlHeaders),
                String.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Something went wrong while retrieving group from legacy XML endpoint");
        }

        if (!xmlMatchesSample(response.getBody(), sampleGroup)) {
            throw new RuntimeException("The returned group doesn't match the expected characteristics");
        }

        UriComponentsBuilder testSamplesEndpoint = uriBuilder.cloneBuilder();
        testSamplesEndpoint.pathSegment("samples", sampleWithinGroup.getAccession());

        response = restTemplate.exchange(testSamplesEndpoint.toUriString(),
                HttpMethod.GET,
                new HttpEntity<>(xmlHeaders),
                String.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Something went wrong while retrieving sample from legacy XML endpoint");
        }

        if (!xmlMatchesSample(response.getBody(), sampleWithinGroup)) {
            throw new RuntimeException("The returned sample doesn't match the expected characteristics");
        }

    }

    @Override
    protected void phaseFive() {
        Sample sampleGroup = getSampleGroup();
        Sample sampleWithinGroup = getSampleWithinGroup();

        HttpHeaders xmlHeaders = new HttpHeaders();
        xmlHeaders.setAccept(Collections.singletonList(MediaType.TEXT_XML));

        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUri(integrationProperties.getBiosampleLegaxyXmlUri());
        UriComponentsBuilder testGroupsEndpoint= uriBuilder.cloneBuilder();
        testGroupsEndpoint.pathSegment("groupsamples", sampleGroup.getAccession());
        testGroupsEndpoint.queryParam("query", "*");

        ResponseEntity<ResultQuery> response = restTemplate.exchange(testGroupsEndpoint.toUriString(),
                HttpMethod.GET,
                new HttpEntity<>(xmlHeaders),
                ResultQuery.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Something went wrong while retrieving group from legacy XML endpoint");
        }

        ResultQuery resultQuery = response.getBody();
        Optional<BioSample> xmlSample = resultQuery.getBioSample().stream()
                .filter(bioSample -> bioSample.getId().equals(sampleWithinGroup.getAccession()))
                .findFirst();
        xmlSample.orElseThrow(() -> new RuntimeException("The legacy XML result query doesn't contain the expected group"));


    }

    private Sample getSampleXMLTest1() {
		String name = "Test XML Sample";
		String accession = "SAMEA999999";
		Instant update = Instant.parse("2016-05-05T11:36:57.00Z");
		Instant release = Instant.parse("2016-04-01T11:36:57.00Z");

		SortedSet<Attribute> attributes = new TreeSet<>();
		attributes.add(
			Attribute.build("organism", "Homo sapiens", "http://purl.obolibrary.org/obo/NCBITaxon_9606", null));
		return Sample.build(name, accession, "self.BiosampleIntegrationTest", release, update, attributes, new TreeSet<>(), new TreeSet<>());
	}

	private Sample getPrivateSampleXMLTest2() {
        String name = "Private XML sample";
        String accession = "SAMEA888888";
		Instant update = Instant.parse("2016-05-05T11:36:57.00Z");
		Instant release = Instant.parse("2116-04-01T11:36:57.00Z");

        SortedSet<Attribute> attributes = new TreeSet<>();
        attributes.add(
                Attribute.build("organism", "Homo sapiens", "http://purl.obolibrary.org/obo/NCBITaxon_9606", null));

        return Sample.build(name, accession, "self.BiosampleIntegrationTest", release, update, attributes, new TreeSet<>(), new TreeSet<>());

    }

    private boolean xmlMatchesSampleInGroup(String serializedXml, Sample sampleInGroup) {
        SAXReader reader = new SAXReader();

        Document xml;
        try {
            xml = reader.read(new StringReader(serializedXml));
        } catch (DocumentException e) {
            return false;
        }

        Element root = xml.getRootElement();
        if (!XmlPathBuilder.of(root).path("ResultQuery").exists()) {
            return false;
        }

        String sampleWithinGroupAccession = XmlPathBuilder.of(root).path("ResultQuery", "BioSample").attribute("id");

        return sampleWithinGroupAccession.equals(sampleInGroup.getAccession());
    }

    private boolean xmlMatchesSample(String serializedXml, Sample referenceSample) {

        SAXReader reader = new SAXReader();

        Document xml;
        try {
            xml = reader.read(new StringReader(serializedXml));
        } catch (DocumentException e) {
            return false;
        }

        Element root = xml.getRootElement();
        if (!XmlPathBuilder.of(root).element().getName().equals("BioSample")) {
            return false;
        }

        String sampleAccession = XmlPathBuilder.of(root).attribute("id");
        Element sampleNameElement = XmlPathBuilder.of(root).path("Property").element();
        String sampleName = XmlPathBuilder.of(sampleNameElement).path("QualifiedValue", "Value").text();

        return sampleAccession.equals(referenceSample.getAccession()) && sampleName.equals(referenceSample.getName());

    }


    private Sample getSampleWithinGroup() {
        String name = "Sample part of group";
        String accession = "SAMEA777777";
        Instant update = Instant.parse("2016-05-05T11:36:57.00Z");
        Instant release = Instant.parse("2016-04-01T11:36:57.00Z");

        SortedSet<Attribute> attributes = new TreeSet<>();


        return Sample.build(name, accession, "self.BiosampleIntegrationTest", release, update, attributes, new TreeSet<>(), new TreeSet<>());

    }

    private Sample getSampleGroup() {
        String name = "Test XML sample group";
        String accession = "SAMEG001122";
        Instant update = Instant.parse("2016-05-05T11:36:57.00Z");
        Instant release = Instant.parse("2016-04-01T11:36:57.00Z");

        SortedSet<Relationship> relationships = new TreeSet<>();
        relationships.add(Relationship.build(accession, "has member", getSampleWithinGroup().getAccession()));

        return Sample.build(name, accession, "self.BiosampleIntegrationTest", release, update, new TreeSet<>(), relationships, new TreeSet<>());
    }
}
