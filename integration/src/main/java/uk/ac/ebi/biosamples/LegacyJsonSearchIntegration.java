package uk.ac.ebi.biosamples;

import com.jayway.jsonpath.JsonPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Profile;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.client.Hop;
import org.springframework.hateoas.client.Traverson;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.*;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.springframework.hateoas.MediaTypes.HAL_JSON;

@Component
@Profile({"default"})
public class LegacyJsonSearchIntegration extends AbstractIntegration {

    private final RestTemplate restTemplate;
    private final IntegrationProperties integrationProperties;
    private final LegacyJsonSearchTester jsonSearchTester;

    Logger log = LoggerFactory.getLogger(getClass());

    public LegacyJsonSearchIntegration(BioSamplesClient client,
                                RestTemplateBuilder restTemplateBuilder,
                                IntegrationProperties integrationProperties) {
        super(client);
        this.restTemplate = restTemplateBuilder.build();
        this.integrationProperties = integrationProperties;
        this.jsonSearchTester = new LegacyJsonSearchTester(log, client, this.restTemplate, this.integrationProperties);

    }

    @Override
    protected void phaseOne() {

        jsonSearchTester.registerTestSamplesInBioSamples();

    }

    /**
     * Check retrieve of samples/groups from corresponding endpoint, and containing correct informations
     */
    @Override
    protected void phaseTwo() {

        jsonSearchTester.itShouldFindTheSampleByAccessionUsingLegacyEndpoint();
        jsonSearchTester.itShouldFindGroupByAccessionUsingLegacyEndpoint();
        jsonSearchTester.itShoudlFindAllSampleDetailsInTheJSON();
        jsonSearchTester.itShouldFindAllGroupDetailsInTheJSON();

    }

    @Override
    protected void phaseThree() {
         jsonSearchTester.itShouldBeAbleToMoveUsingLinks();
    }

    @Override
    protected void phaseFour() {
         jsonSearchTester.itShouldFindSampleSearchingByAccession();
         jsonSearchTester.itShouldFindSampleSearchingByFirstSampleInGroup();
         jsonSearchTester.itShouldFindSampleSearchingByText();
         jsonSearchTester.itShouldFindOnlySamplesWhenSearchingForSamples();
         jsonSearchTester.itShouldFindSampleSearchingByAccessionAndGroup();
         jsonSearchTester.itShouldFindGroupSearchingByAccession();
         jsonSearchTester.itShouldFindGroupSearchingByText();
         jsonSearchTester.itShouldFindOnlyGroupWhenSearchingForGroups();
    }

    @Override
    protected void phaseFive() {

    }

    private class LegacyJsonSearchTester {
        private final Logger log;
        private final BioSamplesClient client;
        private final RestTemplate restTemplate;
        private final IntegrationProperties integrationProperties;

        public LegacyJsonSearchTester(Logger log,
                               BioSamplesClient client,
                               RestTemplate restTemplate,
                               IntegrationProperties integrationProperties) {
            this.log = log;
            this.client = client;
            this.restTemplate = restTemplate;
            this.integrationProperties = integrationProperties;
        }

        public void registerTestSamplesInBioSamples() {

            Sample groupContainegSAMEA911 = TestSampleGenerator.getGroupContainegSAMEA911();
            Optional<Resource<Sample>> optional = this.client.fetchSampleResource(groupContainegSAMEA911.getAccession());
            if (optional.isPresent()) {
                throw new RuntimeException("Found existing "+groupContainegSAMEA911.getAccession());
            }

            Resource<Sample> groupResource = this.client.persistSampleResource(groupContainegSAMEA911);
            // The result and the submitted will not be equal because of the new inverse relation created automatically
            if (!groupContainegSAMEA911.getAccession().equals(groupResource.getContent().getAccession())) {
                throw new RuntimeException("Expected response to equal submission");
            }



            Sample sampleWithinGroup = TestSampleGenerator.getSampleMemberOfGroupWithExternalRelations();
            optional = this.client.fetchSampleResource(sampleWithinGroup.getAccession());
            if (optional.isPresent()) {
                throw new RuntimeException("Found existing "+sampleWithinGroup.getAccession());
            }

            Resource<Sample> sampleWithinGroupResource = this.client.persistSampleResource(sampleWithinGroup);
            // The result and the submitted will not be equal because of the new inverse relation created automatically
            if (!sampleWithinGroup.getAccession().equals(sampleWithinGroupResource.getContent().getAccession())) {
                throw new RuntimeException("Expected response to equal submission");
            }


            Sample sampleWithFullDetails  = TestSampleGenerator.getSampleFullOfDetails();
            optional = this.client.fetchSampleResource(sampleWithFullDetails.getAccession());
            if (optional.isPresent()) {
                throw new RuntimeException("Found existing "+sampleWithFullDetails.getAccession());
            }

            Resource<Sample> sampleWithFullDetailsResource = this.client.persistSampleResource(sampleWithFullDetails, false, true);
            // The result and the submitted will not be equal because of the new inverse relation created automatically
            if (!sampleWithFullDetails.getAccession().equals(sampleWithFullDetailsResource.getContent().getAccession())) {
                throw new RuntimeException("Expected response to equal submission");
            }

            Sample groupWithFullDetails  = TestSampleGenerator.getGroupFullOfDetails();
            optional = this.client.fetchSampleResource(groupWithFullDetails.getAccession());
            if (optional.isPresent()) {
                throw new RuntimeException("Found existing "+groupWithFullDetails.getAccession());
            }

            Resource<Sample> groupWithFullDetailsResource = this.client.persistSampleResource(groupWithFullDetails,false,true);
            // The result and the submitted will not be equal because of the new inverse relation created automatically
            if (!groupWithFullDetails.getAccession().equals(groupWithFullDetailsResource.getContent().getAccession())) {
                throw new RuntimeException("Expected response to equal submission");
            }


        }

        public void itShouldFindTheSampleByAccessionUsingLegacyEndpoint() {

            Sample testSample = TestSampleGenerator.getSampleMemberOfGroupWithExternalRelations();

            log.info(String.format("Searching sample %s using legacy json api", testSample.getAccession()));
            UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUri(integrationProperties.getBiosamplesLegacyJSONUri());
            uriBuilder.pathSegment("samples", testSample.getAccession());
            given().accept("application/hal+json")
                    .when()
                        .get(uriBuilder.toUriString())
                    .then()
                        .statusCode(200)
                        .body("accession", equalTo(testSample.getAccession()));


            log.info(String.format("Sample %s found correctly", testSample.getAccession()));

        }

        public void itShouldFindGroupByAccessionUsingLegacyEndpoint() {
            Sample testGroup = TestSampleGenerator.getGroupContainegSAMEA911();

            log.info("Searching sample " + testGroup.getAccession() + " using legacy json api");
            UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUri(integrationProperties.getBiosamplesLegacyJSONUri());
            uriBuilder.pathSegment("groups", testGroup.getAccession());

            given().accept("application/hal+json")
                    .when()
                        .get(uriBuilder.toUriString())
                    .then()
                        .statusCode(200)
                        .body("accession", equalTo(testGroup.getAccession()));

            log.info("Group " + testGroup.getAccession() + " correctly found with the JSON legacy API");

        }

        public void itShoudlFindAllSampleDetailsInTheJSON() {
            Sample testSample = TestSampleGenerator.getSampleMemberOfGroupWithExternalRelations();

            log.info("Verifying sample " + testSample.getAccession() + " contains all details in the legacy JSON api");
            UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUri(integrationProperties.getBiosamplesLegacyJSONUri());
            uriBuilder.pathSegment("samples", testSample.getAccession());

            given().accept("application/hal+json")
                    .when()
                        .get(uriBuilder.toUriString())
                    .then()
                        .statusCode(200)
                        .body("$", allOf(
                                hasKey("accession"),
                                hasKey("characteristics"),
                                hasKey("externalReferences"),
                                hasKey("_links")));

            log.info("Sample " + testSample.getAccession() + " has all expected fields");

        }

        public void itShouldFindAllGroupDetailsInTheJSON() {
            Sample testGroup = TestSampleGenerator.getGroupContainegSAMEA911();

            log.info("Verifying group " + testGroup.getAccession() + " contains all details in the legacy JSON api");
            UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUri(integrationProperties.getBiosamplesLegacyJSONUri());
            uriBuilder.pathSegment("groups", testGroup.getAccession());

            given().accept("application/hal+json")
                    .when()
                    .get(uriBuilder.toUriString())
                    .then()
                    .statusCode(200)
                    .body("$", allOf(
                            hasKey("accession"),
                            hasKey("characteristics"),
                            hasKey("samples"),
                            hasKey("_links")))
                    .body("characteristics", allOf(hasKey("origin donor"), hasKey("origin cell-line")));

            log.info("Sample " + testGroup.getAccession() + " has all expected fields");

        }

        public void itShouldBeAbleToMoveUsingLinks() {

            Sample testSample = TestSampleGenerator.getSampleMemberOfGroupWithExternalRelations();
            Sample testGroup = TestSampleGenerator.getGroupContainegSAMEA911();

            log.info("Verifying is possible to navigate from sample to group and back to sample");
            UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUri(integrationProperties.getBiosamplesLegacyJSONUri());
            uriBuilder.pathSegment("samples", testSample.getAccession());

            Traverson.TraversalBuilder traversalBuilder = new Traverson(uriBuilder.build().toUri(), HAL_JSON)
                    .follow("relations", "groups");

            String groupJson = traversalBuilder.toObject(String.class);
            assertEquals(
                    JsonPath.parse(groupJson).read("$._embedded.groupsrelations[0].accession"),
                    testGroup.getAccession());

            String sampleJson = traversalBuilder.follow("$._embedded.groupsrelations[0]._links.samples.href").toObject(String.class);
            assertEquals(
                    JsonPath.parse(sampleJson).read("$._embedded.samplesrelations[0].accession"),
                    testSample.getAccession());

            String externalLinkJson = traversalBuilder.follow("$._embedded.samplesrelations[0]._links.externalLinks.href").toObject(String.class);
            assertEquals(
                    JsonPath.parse(externalLinkJson).read("$._embedded.externallinksrelations[0].url"),
                    testSample.getExternalReferences().first().getUrl());

        }

        public void itShouldFindSampleSearchingByAccession() {

            Sample testSample = TestSampleGenerator.getSampleMemberOfGroupWithExternalRelations();

            log.info("Search sample by accession in the legacy JSON");
            UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUri(integrationProperties.getBiosamplesLegacyJSONUri());

            Traverson.TraversalBuilder traversalBuilder = new Traverson(uriBuilder.build().toUri(), HAL_JSON)
                    .follow("samples","search").follow(Hop.rel("findByAccession").withParameter("accession", testSample.getAccession()));

            assertEquals(traversalBuilder.toObject("$._embedded.samples[0].accession"), testSample.getAccession());
            assertEquals((int) traversalBuilder.toObject("$.page.totalElements"), 1);

        }

        public void itShouldFindSampleSearchingByFirstSampleInGroup() {
            Sample testSample = TestSampleGenerator.getSampleMemberOfGroupWithExternalRelations();
            Sample testGroup = TestSampleGenerator.getGroupContainegSAMEA911();

            log.info("Search first sample in a group in legacy JSON");

            Traverson.TraversalBuilder traversalBuilder = new Traverson(integrationProperties.getBiosamplesLegacyJSONUri(), HAL_JSON)
                    .follow("samples", "search").follow(Hop.rel("findFirstByGroupsContains").withParameter("group", testGroup.getAccession()));

            assertEquals(traversalBuilder.toObject("$.accession"), testSample.getAccession());

        }

        public void itShouldFindSampleSearchingByText() {
            Sample testSample = TestSampleGenerator.getSampleMemberOfGroupWithExternalRelations();

            log.info("Search sample by text in the legacy JSON");

            Traverson.TraversalBuilder traversalBuilder = new Traverson(integrationProperties.getBiosamplesLegacyJSONUri(), HAL_JSON)
                    .follow("samples", "search")
                    .follow(Hop.rel("findByText")
                            .withParameter("text", "Homo sapiens")
                            .withParameter("size", 100)
                    );

            assertThat(traversalBuilder.toObject("$._embedded.samples[?(@.accession=='"+ testSample.getAccession() +"')].accession"), contains(testSample.getAccession()));

        }

        public void itShouldFindOnlySamplesWhenSearchingForSamples() {

            log.info("Check sample search only returns samples legacy JSON");

            Traverson.TraversalBuilder traversalBuilder = new Traverson(integrationProperties.getBiosamplesLegacyJSONUri(), HAL_JSON)
                    .follow("samples", "search")
                    .follow(Hop.rel("findByText")
                            .withParameter("text", "SAMEG.*")
                            .withParameter("size", 100)
                    );

            List<String> accessions = traversalBuilder.toObject("$._embedded.samples.*.accession");
            assertFalse(accessions.stream().anyMatch(a -> a.startsWith("SAMEG")));

        }

        public void itShouldFindSampleSearchingByAccessionAndGroup() {
            Sample testSample = TestSampleGenerator.getSampleMemberOfGroupWithExternalRelations();
            Sample testGroup = TestSampleGenerator.getGroupContainegSAMEA911();

            log.info("Search sample by accession and group in the legacy JSON");

            Traverson.TraversalBuilder traversalBuilder = new Traverson(integrationProperties.getBiosamplesLegacyJSONUri(), HAL_JSON)
                    .follow("samples", "search")
                    .follow(Hop.rel("findByAccessionAndGroups")
                            .withParameter("accession", testSample.getAccession())
                            .withParameter("group", testGroup.getAccession())
                            .withParameter("size", 100)
                    );

            assertEquals(traversalBuilder.toObject("$._embedded.samples[0].accession"), testSample.getAccession());
        }

        public void itShouldFindGroupSearchingByAccession() {
            Sample testGroup = TestSampleGenerator.getGroupContainegSAMEA911();

            log.info("Search group by accession and in the legacy JSON");

            Traverson.TraversalBuilder traversalBuilder = new Traverson(integrationProperties.getBiosamplesLegacyJSONUri(), HAL_JSON)
                    .follow("groups", "search")
                    .follow(Hop.rel("findByAccession")
                            .withParameter("accession", testGroup.getAccession())
                            .withParameter("size", 100)
                    );

            assertEquals(traversalBuilder.toObject("$._embedded.groups[0].accession"), testGroup.getAccession());
            assertEquals((int) traversalBuilder.toObject("$.page.size"), 100);
        }

        public void itShouldFindGroupSearchingByText() {

            Sample testGroup = TestSampleGenerator.getGroupContainegSAMEA911();

            log.info("Search group by text and in the legacy JSON");

            Traverson.TraversalBuilder traversalBuilder = new Traverson(integrationProperties.getBiosamplesLegacyJSONUri(), HAL_JSON)
                    .follow("groups", "search")
                    .follow(Hop.rel("findByKeywords")
                            .withParameter("keyword", "Some donor")
                            .withParameter("size", 100)
                    );

            assertEquals(traversalBuilder.toObject("$._embedded.groups[0].accession"), testGroup.getAccession());
            assertEquals((int) traversalBuilder.toObject("$.page.size"), 100);
        }

        public void itShouldFindOnlyGroupWhenSearchingForGroups() {

            log.info("Search group by text should return only groups in the legacy JSON");

            Traverson.TraversalBuilder traversalBuilder = new Traverson(integrationProperties.getBiosamplesLegacyJSONUri(), HAL_JSON)
                    .follow("groups", "search")
                    .follow(Hop.rel("findByKeywords")
                            .withParameter("keyword", "*:*")
                            .withParameter("size", 100)
                    );

            List<String> accessions = traversalBuilder.toObject("$._embedded.groups.*.accession");
            assertTrue(accessions.stream().allMatch(acc -> acc.startsWith("SAMEG")));

        }
    }

    private static class TestSampleGenerator {

        private final static String submissionDomain = "self.BiosampleIntegrationTest";

        public static Sample getSampleMemberOfGroupWithExternalRelations() {
            String name = "TestLegacyJsonSample";
            String accession = "SAMEA911";
            Instant update = Instant.parse("2016-05-05T11:36:57.00Z");
            Instant release = Instant.parse("2016-04-01T11:36:57.00Z");

            SortedSet<Attribute> attributes = new TreeSet<>();
            attributes.add(
                    Attribute.build("organism", "Homo sapiens", "http://purl.obolibrary.org/obo/NCBITaxon_9606", null));
            SortedSet<ExternalReference> externalReferences = new TreeSet<>();
            externalReferences.add(
                    ExternalReference.build("http://hPSCreg.eu/cell-lines/PZIF-001")
            );
            SortedSet<Relationship> relationships = new TreeSet<>();
            relationships.add(Relationship.build("SAMEG199", "has member", "SAMEA911"));

            return Sample.build(name, accession, "self.BiosampleIntegrationTest", release, update, attributes, relationships, externalReferences);
        }

        public static Sample getGroupContainegSAMEA911() {
            String name = "TestLegacyJsonGroup";
            String accession = "SAMEG199";
            Instant update = Instant.parse("2016-05-05T11:36:57.00Z");
            Instant release = Instant.parse("2016-04-01T11:36:57.00Z");

            SortedSet<Attribute> attributes = new TreeSet<>();
            attributes.add( Attribute.build("origin donor", "Some donor", null, null) );
            attributes.add( Attribute.build("origin cell-line", "Some cell line", null, null) );

            SortedSet<Relationship> relationships = new TreeSet<>();
            relationships.add(Relationship.build("SAMEG199", "has member", "SAMEA911"));

            return Sample.build(name, accession, "self.BiosampleIntegrationTest", release, update, 
            		attributes, relationships, null, 
            		null, null, null);

        }

        public static Sample getSampleFullOfDetails() {
            String name = "TestLegacyJsonSampleFullOfDetails";
            String accession = "SAMN1234567890";
            String domain = "self.BiosampleIntegrationTest";
            Instant update = Instant.parse("2016-05-05T11:36:57.00Z");
            Instant release = Instant.parse("2016-04-01T11:36:57.00Z");

            SortedSet<Attribute> attributes = new TreeSet<>();
            SortedSet<Relationship> relationships = new TreeSet<>();
            SortedSet<ExternalReference> externalReferences = new TreeSet<>();

            SortedSet<Organization> organizations = new TreeSet<>();
            organizations.add(new Organization.Builder()
                    .name("Jo Bloggs Inc")
                    .role("user")
                    .address("some address")
                    .role("submitter")
                    .email("help@jobloggs.com")
                    .url("http://www.jobloggs.com")
                    .build());

            SortedSet<Contact> contacts = new TreeSet<>();
            contacts.add(new Contact.Builder()
                    .firstName("Jo")
                    .lastName("Bloggs")
                    .name("Joe Bloggs")
                    .role("Submitter")
                    .email("jobloggs@joblogs.com")
                    .affiliation("test affiliation")
                    .midInitials("JB")
                    .build());

            SortedSet<Publication> publications = new TreeSet<>();
            publications.add(new Publication.Builder()
                    .doi("10.1093/nar/gkt1081")
                    .pubmed_id("24265224")
                    .build());

            return Sample.build(name, accession, domain, release, update, attributes, relationships, externalReferences, organizations, contacts, publications);
        }


        public static Sample getGroupFullOfDetails() {
            String name = "TestLegacyJsonGroupFullOfDetails";
            String accession = "SAMEG0987654321";
            String domain = "self.BiosampleIntegrationTest";
            Instant update = Instant.parse("2016-05-05T11:36:57.00Z");
            Instant release = Instant.parse("2016-04-01T11:36:57.00Z");

            SortedSet<Attribute> attributes = new TreeSet<>();
            SortedSet<Relationship> relationships = new TreeSet<>();
            SortedSet<ExternalReference> externalReferences = new TreeSet<>();

            SortedSet<Organization> organizations = new TreeSet<>();
            organizations.add(new Organization.Builder()
                    .name("Jo Bloggs Inc")
                    .role("user")
                    .address("some address")
                    .role("submitter")
                    .email("help@jobloggs.com")
                    .url("http://www.jobloggs.com")
                    .build());

            SortedSet<Contact> contacts = new TreeSet<>();
            contacts.add(new Contact.Builder()
                    .firstName("Jo")
                    .lastName("Bloggs")
                    .name("Joe Bloggs")
                    .role("Submitter")
                    .email("jobloggs@joblogs.com")
                    .affiliation("test affiliation")
                    .midInitials("JB")
                    .build());

            SortedSet<Publication> publications = new TreeSet<>();
            publications.add(new Publication.Builder()
                    .doi("10.1093/nar/gkt1081")
                    .pubmed_id("24265224")
                    .build());

            return Sample.build(name, accession, domain, release, update, attributes, relationships, externalReferences, organizations, contacts, publications);
        }
    }

}
