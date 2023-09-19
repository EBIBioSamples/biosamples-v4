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
@Order(6)
// @Profile({"default", "rest"})
public class RestIntegration extends AbstractIntegration {
  private final Logger log = LoggerFactory.getLogger(getClass());
  private final RestTemplate restTemplate;
  private final BioSamplesProperties clientProperties;
  private final BioSamplesClient annonymousClient;
  private final BioSamplesClient webinClient;

  public RestIntegration(
      final BioSamplesClient client,
      final RestTemplateBuilder restTemplateBuilder,
      final BioSamplesProperties clientProperties,
      @Qualifier("WEBINCLIENT") final BioSamplesClient webinClient) {
    super(client, webinClient);
    restTemplate = restTemplateBuilder.build();
    this.clientProperties = clientProperties;
    this.webinClient = webinClient;
    annonymousClient =
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
    final Sample testSample = getSampleTest1();
    final Optional<Sample> optionalSample = fetchUniqueSampleByName(testSample.getName());

    if (optionalSample.isPresent()) {
      throw new IntegrationTestFailException(
          "RestIntegration test sample should not be available during phase 1", Phase.ONE);
    } else {
      final EntityModel<Sample> resource = client.persistSampleResource(testSample);
      final Sample sampleContent = resource.getContent();
      final Sample testSampleWithAccession =
          Sample.Builder.fromSample(testSample)
              .withAccession(Objects.requireNonNull(sampleContent).getAccession())
              .withStatus(sampleContent.getStatus())
              .build();

      // trying to post the same sample back again to see PUT is working fine as the sample at this
      // stage has an accession
      final Attribute sraAccessionAttribute =
          resource.getContent().getAttributes().stream()
              .filter(attribute -> attribute.getType().equals("SRA accession"))
              .findFirst()
              .get();

      testSampleWithAccession.getAttributes().add(sraAccessionAttribute);

      final EntityModel<Sample> resourceAfterRepost =
          client.persistSampleResource(testSampleWithAccession);
      final Sample repostedSampleContent = resourceAfterRepost.getContent();

      if (!testSampleWithAccession.equals(sampleContent)) {
        throw new IntegrationTestFailException(
            "Expected response (" + sampleContent + ") to equal submission (" + testSample + ")");
      }

      if (!testSampleWithAccession.equals(repostedSampleContent)) {
        throw new IntegrationTestFailException(
            "Expected response ("
                + repostedSampleContent
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
    postSampleWithAccessionShouldReturnABadRequestResponse();

    final Sample sampleTest1 = getSampleTest1();
    // get to check it worked
    final Optional<Sample> optionalSample = fetchUniqueSampleByName(sampleTest1.getName());
    if (optionalSample.isEmpty()) {
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

    // fetch the already submitted test sample 1
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
      // prepare test sample 1 for resubmission with attached accession
      sampleTest1 =
          Sample.Builder.fromSample(sampleTest1)
              .withAccession(retrievedFromIterable.getAccession())
              .build();
    }

    // put the second sample in
    final EntityModel<Sample> sampleTest2Resource =
        client.persistSampleResource(sampleTest2, false, true);

    // get the accession and sra accession of test sample 2
    final Sample sampleTest2ResourceContent = sampleTest2Resource.getContent();
    final String sampleTest2Accession =
        Objects.requireNonNull(sampleTest2ResourceContent).getAccession();
    final Attribute sraAccessionAttributeForSampleTest2 =
        sampleTest2ResourceContent.getAttributes().stream()
            .filter(attribute -> attribute.getType().equals("SRA accession"))
            .findFirst()
            .get();
    sampleTest2.getAttributes().add(sraAccessionAttributeForSampleTest2);

    // put a version that is private and also update relationships
    final SortedSet<Relationship> relationships = new TreeSet<>();

    relationships.add(
        Relationship.build(sampleTest1.getAccession(), "derived from", sampleTest2Accession));

    // prepare the test sample 1 and now private
    sampleTest1 =
        new Sample.Builder(sampleTest1.getName(), sampleTest1.getAccession())
            .withTaxId(sampleTest1.getTaxId())
            .withDomain(sampleTest1.getDomain())
            .withRelease("2116-04-01T11:36:57.00Z")
            .withUpdate(sampleTest1.getUpdate())
            .withAttributes(sampleTest1.getAttributes())
            .withRelationships(relationships)
            .withExternalReferences(sampleTest1.getExternalReferences())
            .build();

    final EntityModel<Sample> sampleTest1Resource = client.persistSampleResource(sampleTest1);
    final Sample sampleTest1ResourceContent = sampleTest1Resource.getContent();
    final Attribute sraAccessionAttributeForSampleTest1 =
        sampleTest1ResourceContent.getAttributes().stream()
            .filter(attribute -> attribute.getType().equals("SRA accession"))
            .findFirst()
            .get();

    sampleTest1.getAttributes().add(sraAccessionAttributeForSampleTest1);

    if (!sampleTest1.equals(sampleTest1ResourceContent)) {
      throw new IntegrationTestFailException(
          "Expected response ("
              + sampleTest1ResourceContent
              + ") to equal submission ("
              + sampleTest1
              + ")",
          Phase.THREE);
    }

    // check that it is private
    Optional<EntityModel<Sample>> sampleTest1Optional =
        annonymousClient.fetchSampleResource(sampleTest1.getAccession());

    if (sampleTest1Optional.isPresent()) {
      throw new IntegrationTestFailException(
          "Can access private " + sampleTest1.getAccession() + " as annonymous", Phase.THREE);
    }

    // check that it is accessible, if authorised
    sampleTest1Optional = client.fetchSampleResource(sampleTest1.getAccession());

    if (sampleTest1Optional.isEmpty()) {
      throw new IntegrationTestFailException(
          "Cannot access private " + sampleTest1.getAccession(), Phase.THREE);
    }

    // build sample test 2 with accession for resubmission
    sampleTest2 =
        Sample.Builder.fromSample(sampleTest2)
            .withAccession(sampleTest2Accession)
            .withRelationships(sampleTest1.getRelationships())
            .build();

    final Optional<EntityModel<Sample>> sampleTest2Optional =
        client.fetchSampleResource(sampleTest2Accession);

    if (sampleTest2Optional.isEmpty()) {
      throw new IntegrationTestFailException(
          "Cannot access private " + sampleTest2.getAccession(), Phase.THREE);
    }

    if (!sampleTest2.equals(sampleTest2Optional.get().getContent())) {
      throw new IntegrationTestFailException(
          "Expected response ("
              + sampleTest2ResourceContent
              + ") to equal submission ("
              + sampleTest2
              + ")",
          Phase.TWO);
    }

    // test private sample create and fetch using webin auth
    final Sample webinSampleTest1 = getWebinSampleTest1();
    final EntityModel<Sample> webinSampleResource =
        webinClient.persistSampleResource(webinSampleTest1, false, true);
    final String webinSampleAccession =
        Objects.requireNonNull(webinSampleResource.getContent()).getAccession();
    final Optional<EntityModel<Sample>> webinSamplePostPersistance =
        webinClient.fetchSampleResource(webinSampleAccession);

    if (webinSamplePostPersistance.isEmpty()) {
      throw new IntegrationTestFailException(
          "Private sample submitted using webin auth not retrieved", Phase.THREE);
    } else {
      log.info("Found private sample by webin account");
    }
  }

  @Override
  protected void phaseFour() {
    Sample sampleTest1 = getSampleTest1();
    Sample sampleTest2 = getSampleTest2();

    final Optional<Sample> optionalSample = fetchUniqueSampleByName(sampleTest2.getName());
    if (optionalSample.isEmpty()) {
      throw new IntegrationTestFailException(
          "Cannot access private " + sampleTest2.getName(), Phase.FOUR);
    } else {
      sampleTest2 =
          Sample.Builder.fromSample(sampleTest2)
              .withAccession(optionalSample.get().getAccession())
              .build();

      final SortedSet<Relationship> relationships = new TreeSet<>();

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
    final Optional<EntityModel<Sample>> optional =
        client.fetchSampleResource(sampleTest2.getAccession());

    if (optional.isEmpty()) {
      throw new IntegrationTestFailException("No existing " + sampleTest2.getName(), Phase.FOUR);
    }
    final Sample sampleTest2Rest = optional.get().getContent();
    final Attribute sraAccessionAttributeForSampleTest2 =
        sampleTest2Rest.getAttributes().stream()
            .filter(attribute -> attribute.getType().equals("SRA accession"))
            .findFirst()
            .get();

    sampleTest2.getAttributes().add(sraAccessionAttributeForSampleTest2);

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

    final EntityModel<Sample> resource = client.persistSampleResource(sampleTest1);
    final Attribute sraAccessionAttributeForSampleTest1 =
        resource.getContent().getAttributes().stream()
            .filter(attribute -> attribute.getType().equals("SRA accession"))
            .findFirst()
            .get();

    sampleTest1.getAttributes().add(sraAccessionAttributeForSampleTest1);

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
    final Optional<Sample> optionalSample = fetchUniqueSampleByName(sampleTest2.getName());
    final Sample sample;

    if (optionalSample.isEmpty()) {
      throw new IntegrationTestFailException(
          "Cannot access private " + sampleTest2.getName(), Phase.FOUR);
    } else {
      sample = optionalSample.get();
      sampleTest2 =
          Sample.Builder.fromSample(sampleTest2).withAccession(sample.getAccession()).build();
    }

    final Attribute sraAccessionAttributeForSampleTest2 =
        sample.getAttributes().stream()
            .filter(attribute -> attribute.getType().equals("SRA accession"))
            .findFirst()
            .get();

    sampleTest2.getAttributes().add(sraAccessionAttributeForSampleTest2);

    if (!sampleTest2.equals(sample)) {
      throw new IntegrationTestFailException(
          "Expected response (" + sample + ") to equal submission (" + sampleTest2 + ")",
          Phase.FIVE);
    }
  }

  @Override
  protected void phaseSix() {}

  private Sample getSampleTest1() {
    final String name = "RestIntegration_sample_1";
    final Instant update = Instant.parse("2016-05-05T11:36:57.00Z");
    final Instant release = Instant.parse("2016-04-01T11:36:57.00Z");
    final SortedSet<Attribute> attributes = new TreeSet<>();

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

    final SortedSet<Relationship> relationships = new TreeSet<>();
    final SortedSet<ExternalReference> externalReferences = new TreeSet<>();

    externalReferences.add(ExternalReference.build("http://www.google.com"));

    final SortedSet<Organization> organizations = new TreeSet<>();
    organizations.add(
        new Organization.Builder()
            .name("Jo Bloggs Inc")
            .role("user")
            .email("help@jobloggs.com")
            .url("http://www.jobloggs.com")
            .build());

    final SortedSet<Contact> contacts = new TreeSet<>();

    contacts.add(
        new Contact.Builder()
            .name("Joe Bloggs")
            .role("Submitter")
            .email("jobloggs@joblogs.com")
            .build());

    final SortedSet<Publication> publications = new TreeSet<>();

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
    final String name = "RestIntegration_sample_1";
    final Instant update = Instant.parse("2016-05-05T11:36:57.00Z");
    final Instant release = Instant.parse("2116-04-01T11:36:57.00Z");
    final SortedSet<Attribute> attributes = new TreeSet<>();

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

    final SortedSet<Relationship> relationships = new TreeSet<>();
    final SortedSet<ExternalReference> externalReferences = new TreeSet<>();

    externalReferences.add(ExternalReference.build("http://www.google.com"));

    final SortedSet<Organization> organizations = new TreeSet<>();

    organizations.add(
        new Organization.Builder()
            .name("Jo Bloggs Inc")
            .role("user")
            .email("help@jobloggs.com")
            .url("http://www.jobloggs.com")
            .build());

    final SortedSet<Contact> contacts = new TreeSet<>();

    contacts.add(
        new Contact.Builder()
            .name("Joe Bloggs")
            .role("Submitter")
            .email("jobloggs@joblogs.com")
            .build());

    final SortedSet<Publication> publications = new TreeSet<>();

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
    final String name = "RestIntegration_sample_2";
    final Instant update = Instant.parse("2016-05-05T11:36:57.00Z");
    final Instant release = Instant.parse("2016-04-01T11:36:57.00Z");

    final Publication publication =
        new Publication.Builder().doi("doi").pubmed_id("pubmed_id").build();

    final SortedSet<Attribute> attributes = new TreeSet<>();
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
    final Traverson traverson =
        new Traverson(clientProperties.getBiosamplesClientUri(), MediaTypes.HAL_JSON);
    final Traverson.TraversalBuilder builder = traverson.follow("samples");
    log.info("POSTing sample with accession from " + builder.asLink().getHref());

    final MultiValueMap<String, String> sample = new LinkedMultiValueMap<>();
    sample.add("name", "RestIntegration_sample_3");
    sample.add("accession", "SAMEA09123842");
    sample.add("domain", "self.BiosampleIntegrationTest");
    sample.add("release", "2016-05-05T11:36:57.00Z");
    sample.add("update", "2016-04-01T11:36:57.00Z");

    final HttpHeaders httpHeaders = new HttpHeaders();
    httpHeaders.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
    httpHeaders.setContentType(MediaType.APPLICATION_JSON);

    final HttpEntity<MultiValueMap> entity = new HttpEntity<>(sample, httpHeaders);

    try {
      restTemplate.exchange(builder.asLink().getHref(), HttpMethod.POST, entity, String.class);
    } catch (final HttpStatusCodeException sce) {
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
