/*
* Copyright 2021 EMBL - European Bioinformatics Institute
* Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
* file except in compliance with the License. You may obtain a copy of the License at
* http://www.apache.org/licenses/LICENSE-2.0
* Unless required by applicable law or agreed to in writing, software distributed under the
* License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
* CONDITIONS OF ANY KIND, either express or implied. See the License for the
* specific language governing permissions and limitations under the License.
*/
package uk.ac.ebi.biosamples;

import com.google.common.collect.Sets;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import javax.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.annotation.Order;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.client.Traverson;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.*;
import uk.ac.ebi.biosamples.utils.IntegrationTestFailException;

@Component
@Order(2)
// @Profile({"default", "rest"})
public class RestIntegration extends AbstractIntegration {

  private Logger log = LoggerFactory.getLogger(this.getClass());
  private final RestTemplate restTemplate;
  private BioSamplesProperties clientProperties;
  private final BioSamplesClient annonymousClient;
  private BioSamplesClient webinClient;

  public RestIntegration(
      BioSamplesClient client,
      RestTemplateBuilder restTemplateBuilder,
      BioSamplesProperties clientProperties,
      @Qualifier("WEBINCLIENT") BioSamplesClient webinClient) {
    super(client, webinClient);
    this.restTemplate = restTemplateBuilder.build();
    this.clientProperties = clientProperties;
    this.webinClient = webinClient;
    this.annonymousClient =
        new BioSamplesClient(
            this.clientProperties.getBiosamplesClientUri(),
            this.clientProperties.getBiosamplesClientUriV2(),
            restTemplateBuilder,
            null,
            null,
            clientProperties);
  }

  @Override
  protected void phaseOne() {
    Sample testSample = getSampleTest1();
    Optional<Sample> optionalSample = fetchUniqueSampleByName(testSample.getName());

    if (optionalSample.isPresent()) {
      throw new IntegrationTestFailException(
          "RestIntegration test sample should not be available during phase 1", Phase.ONE);
    } else {
      EntityModel<Sample> resource = client.persistSampleResource(testSample);
      Sample testSampleWithAccession =
          Sample.Builder.fromSample(testSample)
              .withAccession(Objects.requireNonNull(resource.getContent()).getAccession())
              .build();

      if (!testSampleWithAccession.equals(resource.getContent())) {
        throw new IntegrationTestFailException(
            "Expected response ("
                + resource.getContent()
                + ") to equal submission ("
                + testSample
                + ")");
      }
    }
  }

  @Override
  protected void phaseTwo() {
    // Test POSTing a sample with an accession to /samples should return 400 BAD REQUEST
    // response
    this.postSampleWithAccessionShouldReturnABadRequestResponse();

    Sample sampleTest1 = getSampleTest1();
    // get to check it worked
    Optional<Sample> optionalSample = fetchUniqueSampleByName(sampleTest1.getName());
    if (!optionalSample.isPresent()) {
      throw new IntegrationTestFailException(
          "Cant find sample " + sampleTest1.getName(), Phase.TWO);
    }
    // check the update date

    final Instant now = Instant.now();

    log.info("Now is : " + now);
    log.info("Sample update date is " + optionalSample.get().getUpdate());
    log.info(
        "Difference of time is "
            + Duration.between(now, optionalSample.get().getUpdate()).abs().getSeconds());

    /* if (Duration.between(Instant.now(), optionalSample.get().getUpdate()).abs().getSeconds()
        > 300) {
      throw new IntegrationTestFailException(
          "Update date was not modified to within 300s as intended, "
              + "expected: "
              + Instant.now()
              + "actual: "
              + optionalSample.get().getUpdate()
              + "",
          Phase.TWO);
    }*/
    // disabled because not fully operational
    // checkIfModifiedSince(optional.get());
    // checkIfMatch(optional.get());

    // TODO check If-Unmodified-Since
    // TODO check If-None-Match
  }

  @Override
  protected void phaseThree() throws InterruptedException {
    Sample sampleTest1 = getSampleTest1();
    Sample sampleTest2 = getSampleTest2();
    Sample retrievedFromIterable = null;

    TimeUnit.SECONDS.sleep(2);

    final Iterable<EntityModel<Sample>> optionalSampleIterable =
        client.fetchSampleResourceAll(sampleTest1.getName());
    final Iterator<EntityModel<Sample>> sampleIterator = optionalSampleIterable.iterator();
    boolean found = false;

    while (sampleIterator.hasNext()) {
      retrievedFromIterable = sampleIterator.next().getContent();

      if (retrievedFromIterable.getName().equals(sampleTest1.getName())) {
        found = true;
      }
    }

    if (!found) {
      throw new IntegrationTestFailException(
          "Cannot access private " + sampleTest1.getName(), Phase.THREE);
    } else {
      sampleTest1 =
          Sample.Builder.fromSample(sampleTest1)
              .withAccession(retrievedFromIterable.getAccession())
              .build();
    }

    // put the second sample in
    EntityModel<Sample> resource = client.persistSampleResource(sampleTest2, false, true);
    String sample2Accession = Objects.requireNonNull(resource.getContent()).getAccession();

    // put a version that is private and also update relationships
    SortedSet<Relationship> relationships = new TreeSet<>();
    relationships.add(
        Relationship.build(sampleTest1.getAccession(), "derived from", sample2Accession));
    sampleTest1 =
        new Sample.Builder(sampleTest1.getName(), sampleTest1.getAccession())
            .withTaxId(sampleTest1.getTaxId())
            .withDomain(sampleTest1.getDomain())
            .withRelease("2116-04-01T11:36:57.00Z")
            .withUpdate(sampleTest1.getUpdate())
            .withAttributes(sampleTest1.getCharacteristics())
            .withRelationships(relationships)
            .withExternalReferences(sampleTest1.getExternalReferences())
            .build();

    resource = client.persistSampleResource(sampleTest1);
    if (!sampleTest1.equals(resource.getContent())) {
      throw new IntegrationTestFailException(
          "Expected response ("
              + resource.getContent()
              + ") to equal submission ("
              + sampleTest1
              + ")",
          Phase.THREE);
    }

    // check that it is private
    Optional<EntityModel<Sample>> optional =
        annonymousClient.fetchSampleResource(sampleTest1.getAccession());
    if (optional.isPresent()) {
      throw new IntegrationTestFailException(
          "Can access private " + sampleTest1.getAccession() + " as annonymous", Phase.THREE);
    }

    // check that it is accessible, if authorised
    optional = client.fetchSampleResource(sampleTest1.getAccession());
    if (!optional.isPresent()) {
      throw new IntegrationTestFailException(
          "Cannot access private " + sampleTest1.getAccession(), Phase.THREE);
    }

    sampleTest2 =
        Sample.Builder.fromSample(sampleTest2)
            .withAccession(sample2Accession)
            .withRelationships(sampleTest1.getRelationships())
            .build();

    optional = client.fetchSampleResource(sampleTest2.getAccession());
    if (!optional.isPresent()) {
      throw new IntegrationTestFailException(
          "Cannot access private " + sampleTest2.getAccession(), Phase.THREE);
    }

    if (!sampleTest2.equals(optional.get().getContent())) {
      throw new IntegrationTestFailException(
          "Expected response ("
              + resource.getContent()
              + ") to equal submission ("
              + sampleTest2
              + ")",
          Phase.TWO);
    }

    // test private sample create and fetch using webin auth
    Sample webinSampleTest1 = getWebinSampleTest1();
    EntityModel<Sample> webinSampleResource =
        this.webinClient.persistSampleResource(webinSampleTest1, false, true);
    String webinSampleAccession =
        Objects.requireNonNull(webinSampleResource.getContent()).getAccession();

    Optional<EntityModel<Sample>> webinSamplePostPersistance =
        this.webinClient.fetchSampleResource(webinSampleAccession);

    if (!webinSamplePostPersistance.isPresent()) {
      throw new IntegrationTestFailException(
          "Private sample submitted using webin auth not retrieved", Phase.THREE);
    } else {
      log.info("Found private sample by webin account");
    }

    // multiple sample fetch by accessions test, authorized user
    Map<String, EntityModel<Sample>> sampleResourcesMap1 =
        this.webinClient.fetchSampleResourcesByAccessions(
            Arrays.asList(webinSampleAccession, "SAMEA100008", "SAMEA100023"));

    if (sampleResourcesMap1 == null || sampleResourcesMap1.size() == 0) {
      throw new IntegrationTestFailException("Multi sample fetch is not working", Phase.THREE);
    }

    // multiple sample fetch by accessions test - v2, authorized user
    Map<String, EntityModel<Sample>> sampleResourcesV2Map1 =
        this.webinClient.fetchSampleResourcesByAccessionsV2(
            Arrays.asList(webinSampleAccession, "SAMEA100008", "SAMEA100023"));

    if (sampleResourcesV2Map1 == null || sampleResourcesV2Map1.size() == 0) {
      throw new IntegrationTestFailException("Multi sample fetch is not working - V2", Phase.THREE);
    }

    // multiple sample fetch by accessions test, unauthorized user
    Map<String, EntityModel<Sample>> sampleResourcesMap2 =
        this.annonymousClient.fetchSampleResourcesByAccessions(
            Arrays.asList(webinSampleAccession, "SAMEA100008", "SAMEA100023"));

    if (sampleResourcesMap2.size() > 2) {
      throw new IntegrationTestFailException(
          "Multi sample fetch is not working, unauthorized user has access to private samples submitted by other submitters",
          Phase.THREE);
    }

    // multiple sample fetch by accessions test - v2, unauthorized user
    Map<String, EntityModel<Sample>> sampleResourcesV2Map2 =
        this.annonymousClient.fetchSampleResourcesByAccessionsV2(
            Arrays.asList(webinSampleAccession, "SAMEA100008", "SAMEA100023"));

    if (sampleResourcesV2Map2.size() > 2) {
      throw new IntegrationTestFailException(
          "Multi sample fetch is not working - V2, unauthorized user has access to private samples submitted by other submitters",
          Phase.THREE);
    }

    // multiple sample fetch by accessions test, authorized user, all samples not found, partial
    // fetch result
    Map<String, EntityModel<Sample>> sampleResourcesMap3 =
        this.webinClient.fetchSampleResourcesByAccessions(
            Arrays.asList(webinSampleAccession, "SAMEA100008", "SAMEA100023", "SAMEA99999999"));

    if (sampleResourcesMap3.size() > 3) {
      throw new IntegrationTestFailException(
          "Multi sample fetch is not working, request with not found samples failing", Phase.THREE);
    }
  }

  @Override
  protected void phaseFour() {
    Sample sampleTest1 = getSampleTest1();
    Sample sampleTest2 = getSampleTest2();

    Optional<Sample> optionalSample = fetchUniqueSampleByName(sampleTest2.getName());
    if (!optionalSample.isPresent()) {
      throw new IntegrationTestFailException(
          "Cannot access private " + sampleTest2.getName(), Phase.FOUR);
    } else {
      sampleTest2 =
          Sample.Builder.fromSample(sampleTest2)
              .withAccession(optionalSample.get().getAccession())
              .build();

      SortedSet<Relationship> relationships = new TreeSet<>();
      relationships.add(
          Relationship.build(
              optionalSample.get().getRelationships().first().getSource(),
              "derived from",
              optionalSample.get().getAccession()));
      sampleTest1 =
          Sample.Builder.fromSample(sampleTest1)
              .withRelationships(relationships)
              .withAccession(optionalSample.get().getRelationships().first().getSource())
              .build();
    }

    // at this point, the inverse relationship should have been added
    sampleTest2 =
        Sample.Builder.fromSample(sampleTest2)
            .withRelationships(sampleTest1.getRelationships())
            .withNoOrganisations()
            .withNoContacts()
            .build();

    // check that it has the additional relationship added
    // get to check it worked
    Optional<EntityModel<Sample>> optional = client.fetchSampleResource(sampleTest2.getAccession());
    if (!optional.isPresent()) {
      throw new IntegrationTestFailException("No existing " + sampleTest2.getName(), Phase.FOUR);
    }
    Sample sampleTest2Rest = optional.get().getContent();
    // check other details i.e relationship
    if (!sampleTest2.equals(sampleTest2Rest)) {
      throw new IntegrationTestFailException(
          "Expected response (" + sampleTest2Rest + ") to equal submission (" + sampleTest2 + ")",
          Phase.FOUR);
    }
    // check utf -8
    if (!sampleTest2Rest.getCharacteristics().contains(Attribute.build("UTF-8 test", "αβ"))) {
      throw new IntegrationTestFailException("Unable to find UTF-8 characters", Phase.FOUR);
    }
    // check the update date is updated by the system
    if (sampleTest2Rest.getUpdate().equals(sampleTest2.getUpdate())) {
      throw new IntegrationTestFailException(
          "Instead of using provided update date, it should be set by the system", Phase.FOUR);
    }

    // now do another update to delete the relationship
    sampleTest1 =
        Sample.Builder.fromSample(sampleTest1)
            .withRelease("2116-04-01T11:36:57.00Z")
            .withNoRelationships()
            .withNoContacts()
            .withNoPublications()
            .withNoOrganisations()
            .build();
    EntityModel<Sample> resource = client.persistSampleResource(sampleTest1);
    if (!sampleTest1.equals(resource.getContent())) {
      throw new IntegrationTestFailException(
          "Expected response ("
              + resource.getContent()
              + ") to equal submission ("
              + sampleTest1
              + ")",
          Phase.FOUR);
    }
  }

  @Override
  protected void phaseFive() {
    // check that deleting the relationship actually deleted it
    Sample sampleTest2 = getSampleTest2();
    Optional<Sample> optionalSample = fetchUniqueSampleByName(sampleTest2.getName());
    if (!optionalSample.isPresent()) {
      throw new IntegrationTestFailException(
          "Cannot access private " + sampleTest2.getName(), Phase.FOUR);
    } else {
      sampleTest2 =
          Sample.Builder.fromSample(sampleTest2)
              .withAccession(optionalSample.get().getAccession())
              .build();
    }

    if (!sampleTest2.equals(optionalSample.get())) {
      throw new IntegrationTestFailException(
          "Expected response ("
              + optionalSample.get()
              + ") to equal submission ("
              + sampleTest2
              + ")",
          Phase.FIVE);
    }
  }

  @Override
  protected void phaseSix() {}

  private Sample getSampleTest1() {
    String name = "RestIntegration_sample_1";
    Instant update = Instant.parse("2016-05-05T11:36:57.00Z");
    Instant release = Instant.parse("2016-04-01T11:36:57.00Z");

    SortedSet<Attribute> attributes = new TreeSet<>();

    attributes.add(
        Attribute.build(
            "organism", "Homo sapiens", "http://purl.obolibrary.org/obo/NCBITaxon_9606", null));
    attributes.add(Attribute.build("age", "3", null, Collections.emptyList(), "year"));
    attributes.add(Attribute.build("organism part", "lung"));
    attributes.add(Attribute.build("organism part", "heart"));
    attributes.add(
        Attribute.build(
            "sex",
            "female",
            null,
            Sets.newHashSet(
                "http://purl.obolibrary.org/obo/PATO_0000383",
                "http://www.ebi.ac.uk/efo/EFO_0001265"),
            null));

    SortedSet<Relationship> relationships = new TreeSet<>();
    SortedSet<ExternalReference> externalReferences = new TreeSet<>();
    externalReferences.add(ExternalReference.build("http://www.google.com"));

    SortedSet<Organization> organizations = new TreeSet<>();
    organizations.add(
        new Organization.Builder()
            .name("Jo Bloggs Inc")
            .role("user")
            .email("help@jobloggs.com")
            .url("http://www.jobloggs.com")
            .build());

    SortedSet<Contact> contacts = new TreeSet<>();
    contacts.add(
        new Contact.Builder()
            .name("Joe Bloggs")
            .role("Submitter")
            .email("jobloggs@joblogs.com")
            .build());

    SortedSet<Publication> publications = new TreeSet<>();
    publications.add(
        new Publication.Builder().doi("10.1093/nar/gkt1081").pubmed_id("24265224").build());

    return new Sample.Builder(name)
        .withTaxId(9606L)
        .withUpdate(update)
        .withRelease(release)
        .withDomain(defaultIntegrationSubmissionDomain)
        .withAttributes(attributes)
        .withRelationships(relationships)
        .withExternalReferences(externalReferences)
        .withOrganizations(organizations)
        .withContacts(contacts)
        .withPublications(publications)
        .build();
  }

  private Sample getWebinSampleTest1() {
    String name = "RestIntegration_sample_1";
    Instant update = Instant.parse("2016-05-05T11:36:57.00Z");
    Instant release = Instant.parse("2116-04-01T11:36:57.00Z");

    SortedSet<Attribute> attributes = new TreeSet<>();
    attributes.add(
        Attribute.build(
            "organism", "Homo sapiens", "http://purl.obolibrary.org/obo/NCBITaxon_9606", null));
    attributes.add(Attribute.build("age", "3", null, Collections.emptyList(), "year"));
    attributes.add(Attribute.build("organism part", "lung"));
    attributes.add(Attribute.build("organism part", "heart"));
    attributes.add(
        Attribute.build(
            "sex",
            "female",
            null,
            Sets.newHashSet(
                "http://purl.obolibrary.org/obo/PATO_0000383",
                "http://www.ebi.ac.uk/efo/EFO_0001265"),
            null));

    SortedSet<Relationship> relationships = new TreeSet<>();
    SortedSet<ExternalReference> externalReferences = new TreeSet<>();
    externalReferences.add(ExternalReference.build("http://www.google.com"));

    SortedSet<Organization> organizations = new TreeSet<>();
    organizations.add(
        new Organization.Builder()
            .name("Jo Bloggs Inc")
            .role("user")
            .email("help@jobloggs.com")
            .url("http://www.jobloggs.com")
            .build());

    SortedSet<Contact> contacts = new TreeSet<>();
    contacts.add(
        new Contact.Builder()
            .name("Joe Bloggs")
            .role("Submitter")
            .email("jobloggs@joblogs.com")
            .build());

    SortedSet<Publication> publications = new TreeSet<>();
    publications.add(
        new Publication.Builder().doi("10.1093/nar/gkt1081").pubmed_id("24265224").build());

    return new Sample.Builder(name)
        .withUpdate(update)
        .withRelease(release)
        .withWebinSubmissionAccountId("Webin-40894")
        .withAttributes(attributes)
        .withRelationships(relationships)
        .withExternalReferences(externalReferences)
        .withOrganizations(organizations)
        .withContacts(contacts)
        .withPublications(publications)
        .build();
  }

  @PreDestroy
  public void destroy() {
    annonymousClient.close();
  }

  private Sample getSampleTest2() {
    String name = "RestIntegration_sample_2";
    Instant update = Instant.parse("2016-05-05T11:36:57.00Z");
    Instant release = Instant.parse("2016-04-01T11:36:57.00Z");

    Publication publication = new Publication.Builder().doi("doi").pubmed_id("pubmed_id").build();

    SortedSet<Attribute> attributes = new TreeSet<>();
    attributes.add(
        Attribute.build(
            "organism", "Homo sapiens", "http://purl.obolibrary.org/obo/NCBITaxon_9606", null));
    attributes.add(Attribute.build("UTF-8 test", "αβ"));

    return new Sample.Builder(name)
        .withTaxId(9606L)
        .withDomain(defaultIntegrationSubmissionDomain)
        .withRelease(release)
        .withUpdate(update)
        .withPublications(Collections.singletonList(publication))
        .withAttributes(attributes)
        .build();
  }

  private void postSampleWithAccessionShouldReturnABadRequestResponse() {
    Traverson traverson =
        new Traverson(this.clientProperties.getBiosamplesClientUri(), MediaTypes.HAL_JSON);
    Traverson.TraversalBuilder builder = traverson.follow("samples");
    log.info("POSTing sample with accession from " + builder.asLink().getHref());

    MultiValueMap<String, String> sample = new LinkedMultiValueMap<>();
    sample.add("name", "RestIntegration_sample_3");
    sample.add("accession", "SAMEA09123842");
    sample.add("domain", "self.BiosampleIntegrationTest");
    sample.add("release", "2016-05-05T11:36:57.00Z");
    sample.add("update", "2016-04-01T11:36:57.00Z");

    HttpHeaders httpHeaders = new HttpHeaders();
    httpHeaders.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
    httpHeaders.setContentType(MediaType.APPLICATION_JSON);

    HttpEntity<MultiValueMap> entity = new HttpEntity<>(sample, httpHeaders);

    try {
      restTemplate.exchange(builder.asLink().getHref(), HttpMethod.POST, entity, String.class);
    } catch (HttpStatusCodeException sce) {
      if (!sce.getStatusCode().equals(HttpStatus.BAD_REQUEST)) {
        throw new RuntimeException(
            "POSTing to samples endpoint a sample with an accession should return a 400 Bad Request exception");
      }
    }

    log.info(
        String.format(
            "POSTing sample with accession from %s produced a BAD REQUEST as expected and wanted ",
            builder.asLink().getHref()));
  }
}
