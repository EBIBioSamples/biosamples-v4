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
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutionException;
import javax.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.annotation.Order;
import org.springframework.hateoas.EntityModel;
import org.springframework.stereotype.Component;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.*;
import uk.ac.ebi.biosamples.utils.IntegrationTestFailException;

@Component
@Order(2)
public class RestIntegrationV2 extends AbstractIntegration {
  private Logger log = LoggerFactory.getLogger(this.getClass());
  private final BioSamplesClient annonymousClient;
  private BioSamplesClient webinClient;

  public RestIntegrationV2(
      BioSamplesClient client,
      RestTemplateBuilder restTemplateBuilder,
      BioSamplesProperties clientProperties,
      @Qualifier("WEBINCLIENT") BioSamplesClient webinClient) {
    super(client, webinClient);
    this.webinClient = webinClient;
    this.annonymousClient =
        new BioSamplesClient(
            clientProperties.getBiosamplesClientUri(),
            clientProperties.getBiosamplesClientUriV2(),
            restTemplateBuilder,
            null,
            null,
            clientProperties);
  }

  @Override
  protected void phaseOne() {}

  @Override
  protected void phaseTwo() {}

  @Override
  protected void phaseThree() {}

  @Override
  protected void phaseFour() {}

  @Override
  protected void phaseFive() {}

  @Override
  protected void phaseSix() throws ExecutionException, InterruptedException {
    Sample webinSampleTest1 = getWebinSampleTest1();
    List<Sample> webinSampleResource =
        this.webinClient.persistSampleResourceV2(Collections.singletonList(webinSampleTest1));
    String webinSampleAccession = Objects.requireNonNull(webinSampleResource.get(0)).getAccession();

    Optional<EntityModel<Sample>> webinSamplePostPersistance =
        this.webinClient.fetchSampleResource(webinSampleAccession);

    if (!webinSamplePostPersistance.isPresent()) {
      throw new IntegrationTestFailException(
          "Private sample submitted using webin auth not retrieved", Phase.SIX);
    }

    Sample webinSampleMinimalInfo = getWebinSampleMinimalInfo();
    Map<String, String> sampleAccessionToNameMap =
        this.webinClient.bulkAccessionV2(Collections.singletonList(webinSampleMinimalInfo));

    if (sampleAccessionToNameMap.size() == 0) {
      throw new IntegrationTestFailException("Bulk accession is not working for V2", Phase.SIX);
    }

    // test private sample create and fetch using webin auth - v2
    try {
      Sample webinTestSampleV2Submission = getWebinSampleTest1();
      List<Sample> apiResponseSampleResourceList =
          this.webinClient.persistSampleResourceV2(
              Collections.singletonList(webinTestSampleV2Submission));
      String apiResponseSampleAccession1 =
          Objects.requireNonNull(apiResponseSampleResourceList.get(0)).getAccession();

      Map<String, Sample> apiResponseV2SampleBulkFetch =
          this.webinClient.fetchSampleResourcesByAccessionsV2(
              Collections.singletonList(apiResponseSampleAccession1));

      if (apiResponseV2SampleBulkFetch.isEmpty()) {
        throw new IntegrationTestFailException(
            "Private sample submitted using webin auth using the V2 end point is not retrieved",
            Phase.THREE);
      } else {
        log.info("Found private sample by webin account");
        final Collection<Sample> foundSamples = apiResponseV2SampleBulkFetch.values();

        foundSamples.forEach(sample -> log.info(String.valueOf(sample)));
      }
    } catch (final Exception e) {
      throw new IntegrationTestFailException("V2 persist and fetch tests failed", Phase.SIX);
    }

    // multiple sample fetch by accessions test - v2, authorized user
    Map<String, Sample> sampleResourcesV2Map1 =
        this.webinClient.fetchSampleResourcesByAccessionsV2(
            Arrays.asList(webinSampleAccession, "SAMEA100008", "SAMEA100023"));

    if (sampleResourcesV2Map1 == null || sampleResourcesV2Map1.size() == 0) {
      throw new IntegrationTestFailException("Multi sample fetch is not working - V2", Phase.SIX);
    }

    // multiple sample fetch by accessions test - v2, unauthorized user
    Map<String, Sample> sampleResourcesV2Map2 =
        this.annonymousClient.fetchSampleResourcesByAccessionsV2(
            Arrays.asList(webinSampleAccession, "SAMEA100008", "SAMEA100023"));

    if (sampleResourcesV2Map2.size() > 2) {
      throw new IntegrationTestFailException(
          "Multi sample fetch is not working - V2, unauthorized user has access to private samples submitted by other submitters",
          Phase.SIX);
    }

    // multiple sample fetch by accessions test, authorized user - v2, all samples not found,
    // partial
    // fetch result
    Map<String, Sample> sampleResourcesV2Map3 =
        this.webinClient.fetchSampleResourcesByAccessionsV2(
            Arrays.asList(webinSampleAccession, "SAMEA100008", "SAMEA100023", "SAMEA99999999"));

    if (sampleResourcesV2Map3.size() > 3) {
      throw new IntegrationTestFailException(
          "Multi sample fetch is not working - V2, request with not found samples failing",
          Phase.SIX);
    }
  }

  @PreDestroy
  public void destroy() {
    annonymousClient.close();
  }

  private Sample getWebinSampleTest1() {
    String name = "RestIntegrationWebin_sample_1";
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

  private Sample getWebinSampleMinimalInfo() {
    String name = "RestIntegrationWebin_sample_1";

    return new Sample.Builder(name).build();
  }
}
