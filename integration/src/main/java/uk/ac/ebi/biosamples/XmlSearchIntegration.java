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
import java.util.*;

@Component
@Profile({"default", "test"})
public class XmlSearchIntegration extends AbstractIntegration {
    
    private final RestTemplate restTemplate;
    private final IntegrationProperties integrationProperties;
    private final XmlSearchTester xmlSearchTester;

    Logger log = LoggerFactory.getLogger(getClass());

    public XmlSearchIntegration(BioSamplesClient client,
                                RestTemplateBuilder restTemplateBuilder,
                                IntegrationProperties integrationProperties) {
        super(client);
        this.restTemplate = restTemplateBuilder.build();
        this.integrationProperties = integrationProperties;
        this.xmlSearchTester = new XmlSearchTester(log, client, this.restTemplate, this.integrationProperties);

    }

    @Override
    protected void phaseOne() {

        xmlSearchTester.registerTestSamplesInBioSamples();

    }

    @Override
    protected void phaseTwo() {

        xmlSearchTester.triesToFindSampleUsingClient();

        xmlSearchTester.triesToFindSampleUsingLegacyEndpoint();

        xmlSearchTester.triesAndFailToFindPrivateSampleUsingLegacyEndpoint();

    }


    @Override
    protected void phaseThree() {

//        xmlSearchTester.failsToQueryLegacyEndpointWithoutRequiredQueryParameter();

        xmlSearchTester.failsToAccessLegacyEndpointUsingJsonHeader();

    }

    @Override
    protected void phaseFour() {

        xmlSearchTester.triesToFindGroupUsingLegacyEndpoint();

        xmlSearchTester.triesToFindSamplePartOfGroupUsingLegacyEndpoint();

        xmlSearchTester.searchesForSamplesUsingQueryParametersOnLegacyEndpoint();

        xmlSearchTester.searchesForSamplesUsingDateRangesOnLegacyEndpoint();

        xmlSearchTester.findSamplesUsingNCBIQueryStyle();
    }

    @Override
    protected void phaseFive() {

        xmlSearchTester.getsResultSummaryAsResultOfSearchingSampleInGroup();

    }

    private class XmlSearchTester {

        private final IntegrationProperties integrationProperties;
        private final Logger log;
        private final BioSamplesClient client;
        private final RestTemplate restTemplate;

        public XmlSearchTester(Logger log,
                               BioSamplesClient client,
                               RestTemplate restTemplate,
                               IntegrationProperties integrationProperties) {
            this.log = log;
            this.client = client;
            this.restTemplate = restTemplate;
            this.integrationProperties = integrationProperties;
        }

        public void registerTestSamplesInBioSamples() {

            List<Sample> baseSampleList = Arrays.asList(
                    TestSampleGenerator.getRegularSample(),
                    TestSampleGenerator.getPrivateSample(),
                    TestSampleGenerator.getSampleGroup(),
                    TestSampleGenerator.getSampleWithSpecificUpdateDate()
            );

            for (Sample sample: baseSampleList) {
                Optional<Resource<Sample>> optional = client.fetchSampleResource(sample.getAccession());
                if (optional.isPresent()) {
                    throw new RuntimeException("Found existing "+sample.getAccession());
                }

                Resource<Sample> resource = client.persistSampleResource(sample);
                if (!sample.equals(resource.getContent())) {
                    throw new RuntimeException("Expected response to equal submission");
                }

            }

            Sample sampleWithinGroup = TestSampleGenerator.getSampleWithinGroup();
            Optional<Resource<Sample>> optional = client.fetchSampleResource(sampleWithinGroup.getAccession());
            if (optional.isPresent()) {
                throw new RuntimeException("Found existing "+sampleWithinGroup.getAccession());
            }

            Resource<Sample> sampleWithinGroupResource = client.persistSampleResource(sampleWithinGroup);
            // The result and the submitted will not be equal because of the new inverse relation created automatically
            if (!sampleWithinGroup.getAccession().equals(sampleWithinGroupResource.getContent().getAccession())) {
                throw new RuntimeException("Expected response to equal submission");
            }


        }

        public void triesToFindSampleUsingClient() {
            Sample test1 = TestSampleGenerator.getRegularSample();

            log.info("Check existence of sample " + test1.getAccession());

            Optional<Resource<Sample>> optional = client.fetchSampleResource(test1.getAccession());
            if (!optional.isPresent()) {
                throw new RuntimeException("Expected sample not found "+test1.getAccession());
            }

            log.info("Sample " + test1.getAccession() + " found correctly");
        }

        public void triesToFindSampleUsingLegacyEndpoint() {
            Sample test1 = TestSampleGenerator.getRegularSample();

            log.info(String.format("Searching sample %s using legacy xml api", test1.getAccession()));
            UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUri(integrationProperties.getBiosamplesLegacyXMLUri());
            uriBuilder.pathSegment("samples", test1.getAccession());

            RequestEntity<?> request = RequestEntity
                    .get(uriBuilder.build().toUri())
                    .accept(MediaType.TEXT_XML)
                    .build();

            ResponseEntity<String> responseEntity = restTemplate.exchange(request, String.class);


            if (!responseEntity.getBody().contains(String.format("id=\"%s\"",test1.getAccession()))) {
                throw new RuntimeException("Response body doesn't match expected sample");
            }

            assert responseEntity.getBody().equals(test1);
            log.info(String.format("Sample %s found using legacy xml api", test1.getAccession()));
        }

        public void triesAndFailToFindPrivateSampleUsingLegacyEndpoint() {

            Sample privateSample = TestSampleGenerator.getPrivateSample();

            log.info(String.format("Searching private sample %s using legacy xml api", privateSample.getAccession()));

            UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUri(integrationProperties.getBiosamplesLegacyXMLUri());
            uriBuilder.pathSegment("samples", privateSample.getAccession());

            try {
                RequestEntity<?> sampleRequestEntity = RequestEntity
                        .get(uriBuilder.build().toUri())
                        .accept(MediaType.TEXT_XML)
                        .build();

                ResponseEntity<Sample> sampleResponseEntity = restTemplate.exchange(sampleRequestEntity, Sample.class);
                if (sampleResponseEntity.getStatusCode().equals(HttpStatus.OK)) {
                    throw new RuntimeException(
                            String.format("Sample %s should be not available through the legaxy xml api",
                                    privateSample.getAccession()));
                }
            } catch (HttpClientErrorException e) {
                log.info("e.getStatusCode() = "+e.getStatusCode());
                if (e.getStatusCode().equals(HttpStatus.OK)) {
                    throw new RuntimeException(String.format("Sample %s should be not available through the legaxy xml api", privateSample.getAccession()));
                }
            }
        }

        public void triesToFindGroupUsingLegacyEndpoint() {

            Sample sampleGroup = TestSampleGenerator.getSampleGroup();
            HttpHeaders xmlHeaders = new HttpHeaders();
            xmlHeaders.setAccept(Collections.singletonList(MediaType.TEXT_XML));

            UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUri(integrationProperties.getBiosamplesLegacyXMLUri());

            UriComponentsBuilder testGroupsEndpoint = uriBuilder.cloneBuilder();
            testGroupsEndpoint.pathSegment("groups", sampleGroup.getAccession());


            ResponseEntity<String> response = restTemplate.exchange(testGroupsEndpoint.toUriString(),
                    HttpMethod.GET,
                    new HttpEntity<>(xmlHeaders),
                    String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Something went wrong while retrieving group from legacy XML endpoint");
            }

            if (!XmlMatcher.matchesSample(response.getBody(), sampleGroup)) {
                throw new RuntimeException("The returned group doesn't match the expected characteristics");
            }

        }

        public void triesToFindSamplePartOfGroupUsingLegacyEndpoint() {
            log.info("Find samples part of a group using legacy xml endpoint");

            Sample sampleWithinGroup = TestSampleGenerator.getSampleWithinGroup();
            Sample groupSample = TestSampleGenerator.getSampleGroup();

            HttpHeaders xmlHeaders = new HttpHeaders();
            xmlHeaders.setAccept(Collections.singletonList(MediaType.TEXT_XML));

            UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUri(integrationProperties.getBiosamplesLegacyXMLUri());

            UriComponentsBuilder testSamplesEndpoint = uriBuilder.cloneBuilder();
            testSamplesEndpoint.pathSegment("groupsamples", groupSample.getAccession());

            ResponseEntity<String> response = restTemplate.exchange(testSamplesEndpoint.toUriString(),
                    HttpMethod.GET,
                    new HttpEntity<>(xmlHeaders),
                    String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Something went wrong while retrieving sample from legacy XML endpoint");
            }

            if (!XmlMatcher.matchesSampleInGroup(response.getBody(), sampleWithinGroup)) {
                throw new RuntimeException("The returned sample doesn't match the expected characteristics");
            }
        }

        public void failsToAccessLegacyEndpointUsingJsonHeader() {
            log.info("Try to generate a NOT ACCEPTABLE error using legacy xml samples end-point with application/json accept header");
            UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUri(integrationProperties.getBiosamplesLegacyXMLUri());

            UriComponentsBuilder testBadRequest= uriBuilder.cloneBuilder();
            testBadRequest.pathSegment("samples");

            // Accept application/json header
            HttpHeaders jsonHeaders = new HttpHeaders();
            jsonHeaders.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));


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
        }

        public void failsToQueryLegacyEndpointWithoutRequiredQueryParameter() {
            log.info("Try to generate a BAD REQUEST using legacy xml samples end-point without required parameter");
            UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUri(integrationProperties.getBiosamplesLegacyXMLUri());

            UriComponentsBuilder testBadRequest= uriBuilder.cloneBuilder();
            testBadRequest.pathSegment("samples");

            // Accept text/xml header
            HttpHeaders xmlHeaders = new HttpHeaders();
            xmlHeaders.setAccept(Collections.singletonList(MediaType.TEXT_XML));

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

        }

        public void searchesForSamplesUsingQueryParametersOnLegacyEndpoint() {
            log.info("Search for samples in legacy xml using query parameter");
            UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUri(integrationProperties.getBiosamplesLegacyXMLUri());

            UriComponentsBuilder testBadRequest= uriBuilder.cloneBuilder();
            testBadRequest.pathSegment("samples");

            // Accept text/xml header
            HttpHeaders xmlHeaders = new HttpHeaders();
            xmlHeaders.setAccept(Collections.singletonList(MediaType.TEXT_XML));
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

        public void getsResultSummaryAsResultOfSearchingSampleInGroup() {
            log.info("Get result summary of searching a sample in a group");
            Sample sampleGroup = TestSampleGenerator.getSampleGroup();
            Sample sampleWithinGroup = TestSampleGenerator.getSampleWithinGroup();

            HttpHeaders xmlHeaders = new HttpHeaders();
            xmlHeaders.setAccept(Collections.singletonList(MediaType.TEXT_XML));

            UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUri(integrationProperties.getBiosamplesLegacyXMLUri());
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

        public void searchesForSamplesUsingDateRangesOnLegacyEndpoint() {
            log.info("Search legacy XML using date range query");
            Sample testSample = TestSampleGenerator.getSampleWithSpecificUpdateDate();

            HttpHeaders xmlHeaders = new HttpHeaders();
            xmlHeaders.setAccept(Collections.singletonList(MediaType.TEXT_XML));

            UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUri(integrationProperties.getBiosamplesLegacyXMLUri());
            UriComponentsBuilder legacyXmlSampleSearchEndpoint= uriBuilder.cloneBuilder();
            legacyXmlSampleSearchEndpoint.pathSegment("samples")
                    .queryParam("query", "releasedate:[1980-08-01 TO 1980-08-03]");

            ResponseEntity<ResultQuery> response = restTemplate.exchange(legacyXmlSampleSearchEndpoint.build().toUri().toString(),
                    HttpMethod.GET,
                    new HttpEntity<>(xmlHeaders),
                    ResultQuery.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Something went wrong while retrieving results from legacy XML endpoint");
            }

            ResultQuery resultQuery = response.getBody();
            Optional<BioSample> xmlSample = resultQuery.getBioSample().stream()
                    .filter(bioSample -> bioSample.getId().equals(testSample.getAccession()))
                    .findFirst();
            if (!xmlSample.isPresent()) {
                throw new RuntimeException("The legacy XML result query doesn't contain the expected sample");
            }
        }

        public void findSamplesUsingNCBIQueryStyle() {
            log.info("Search legacy XML using NCBI style query");
            Sample testSample = TestSampleGenerator.getSampleWithSpecificUpdateDate();

            HttpHeaders xmlHeaders = new HttpHeaders();
            xmlHeaders.setAccept(Collections.singletonList(MediaType.TEXT_XML));

            UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUri(integrationProperties.getBiosamplesLegacyXMLUri());
            UriComponentsBuilder legacyXmlSampleSearchEndpoint= uriBuilder.cloneBuilder();
            legacyXmlSampleSearchEndpoint.pathSegment("samples")
                    .queryParam("query", "SAME* AND releasedate:[1980-08-01 TO 2018-08-03]");

            ResponseEntity<ResultQuery> response = restTemplate.exchange(legacyXmlSampleSearchEndpoint.build().toUri().toString(),
                    HttpMethod.GET,
                    new HttpEntity<>(xmlHeaders),
                    ResultQuery.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Something went wrong while retrieving results from legacy XML endpoint");
            }

//            ResultQuery resultQuery = response.getBody();
//            Optional<BioSample> xmlSample = resultQuery.getBioSample().stream()
//                    .filter(bioSample -> bioSample.getId().equals(testSample.getAccession()))
//                    .findFirst();
//            if (!xmlSample.isPresent()) {
//                throw new RuntimeException("The legacy XML result query doesn't contain the expected sample");
//            }
        }
    }

    private static class TestSampleGenerator {

        private final static String submissionDomain = "self.BiosampleIntegrationTest";

        public static Sample getRegularSample() {
            String name = "Test XML Sample";
            String accession = "SAMEA999999";
            Instant update = Instant.parse("2016-05-05T11:36:57.00Z");
            Instant release = Instant.parse("2016-04-01T11:36:57.00Z");

            SortedSet<Attribute> attributes = new TreeSet<>();
            attributes.add(
                    Attribute.build("organism", "Homo sapiens", "http://purl.obolibrary.org/obo/NCBITaxon_9606", null));
            return Sample.build(name, accession, "self.BiosampleIntegrationTest", release, update, attributes, new TreeSet<>(), new TreeSet<>(), null, null, null);
        }

        public static Sample getPrivateSample() {
            String name = "Private XML sample";
            String accession = "SAMEA888888";
            Instant update = Instant.parse("2016-05-05T11:36:57.00Z");
            Instant release = Instant.parse("2116-04-01T11:36:57.00Z");

            SortedSet<Attribute> attributes = new TreeSet<>();
            attributes.add(
                    Attribute.build("organism", "Homo sapiens", "http://purl.obolibrary.org/obo/NCBITaxon_9606", null));

            return Sample.build(name, accession, "self.BiosampleIntegrationTest", release, update, attributes, new TreeSet<>(), new TreeSet<>(), null, null, null);

        }

        public static Sample getSampleWithinGroup() {
            String name = "Sample part of group";
            String accession = "SAMEA777777";
            Instant update = Instant.parse("2016-05-05T11:36:57.00Z");
            Instant release = Instant.parse("2016-04-01T11:36:57.00Z");

            SortedSet<Attribute> attributes = new TreeSet<>();


            return Sample.build(name, accession, "self.BiosampleIntegrationTest", release, update, attributes, new TreeSet<>(), new TreeSet<>(), null, null, null);

        }

        public static Sample getSampleGroup() {
            String name = "Test XML sample group";
            String accession = "SAMEG001122";
            Instant update = Instant.parse("2016-05-05T11:36:57.00Z");
            Instant release = Instant.parse("2016-04-01T11:36:57.00Z");

            SortedSet<Relationship> relationships = new TreeSet<>();
            relationships.add(Relationship.build(accession, "has member", getSampleWithinGroup().getAccession()));

            return Sample.build(name, accession, "self.BiosampleIntegrationTest", release, update, new TreeSet<>(), relationships, new TreeSet<>(), null, null, null);
        }

        public static Sample getSampleWithSpecificUpdateDate() {
            String name = "Test XML sample for update date";
            String accession = "SAME101010";
            Instant update = Instant.now();
            Instant release = Instant.parse("1980-08-02T00:30:00Z");

            return Sample.build(name, accession, submissionDomain, release, update, null, null, null, null, null, null);

        }

    }

    private static class XmlMatcher {

        public static boolean matchesSampleInGroup(String serializedXml, Sample sampleInGroup) {
            SAXReader reader = new SAXReader();

            Document xml;
            try {
                xml = reader.read(new StringReader(serializedXml));
            } catch (DocumentException e) {
                return false;
            }

            Element root = xml.getRootElement();
            if (!root.getName().equals("ResultQuery")) {
                return false;
            }

            String sampleWithinGroupAccession = XmlPathBuilder.of(root).path("BioSample").attribute("id");

            return sampleWithinGroupAccession.equals(sampleInGroup.getAccession());
        }

        public static boolean matchesSample(String serializedXml, Sample referenceSample) {

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
    }


}
