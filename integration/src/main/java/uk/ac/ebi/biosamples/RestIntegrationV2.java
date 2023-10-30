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
  private final Logger log = LoggerFactory.getLogger(getClass());
  private final BioSamplesClient annonymousClient;
  private final BioSamplesClient webinClient;

  public RestIntegrationV2(
      final BioSamplesClient client,
      final RestTemplateBuilder restTemplateBuilder,
      final BioSamplesProperties clientProperties,
      @Qualifier("WEBINCLIENT") final BioSamplesClient webinClient) {
    super(client, webinClient);
    this.webinClient = webinClient;
    annonymousClient =
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
  protected void phaseFive() {
    final Optional<EntityModel<Sample>> sampleSubmittedWithAAPAuth =
        annonymousClient.fetchSampleResource("SAMEA1");
    final Sample sampleSubmittedWithAAPAuthNowAfterPersistenceWithWebinSuperUser =
        webinClient
            .persistSampleResource(sampleSubmittedWithAAPAuth.get().getContent())
            .getContent();

    if (sampleSubmittedWithAAPAuthNowAfterPersistenceWithWebinSuperUser.getAttributes().stream()
        .noneMatch(attribute -> attribute.getType().equals("SRA accession"))) {
      throw new IntegrationTestFailException(
          "Sample must have a SRA accession at this stage", Phase.FIVE);
    }
  }

  // TODO: cleanup this test
  @Override
  protected void phaseSix() throws ExecutionException, InterruptedException {
    final Sample webinSampleTest1 = getWebinSampleTest1();

    // Submit with webin client, no jwt passed
    final List<Sample> webinSampleResource =
        webinClient.persistSampleResourceV2(Collections.singletonList(webinSampleTest1));
    final String webinSampleAccession =
        Objects.requireNonNull(webinSampleResource.get(0)).getAccession();

    final Optional<EntityModel<Sample>> webinSamplePostPersistence =
        webinClient.fetchSampleResource(webinSampleAccession);

    if (!webinSamplePostPersistence.isPresent()) {
      throw new IntegrationTestFailException(
          "Private sample submitted using webin auth not retrieved", Phase.SIX);
    }

    // Submit with public client, pass jwt
    /* List<Sample> webinSampleResource2 =
        this.publicClient.persistSampleResourceV2(
            Collections.singletonList(webinSampleTest1),
            "eyJhbGciOiJSUzI1NiJ9.eyJwcmluY2lwbGUiOiJXZWJpbi01NzE3NiIsInJvbGUiOlsiTUVUQUdFTk9NRV9BTkFMWVNJUyJdLCJleHAiOjE2NjU3NTk0NTQsImlhdCI6MTY2NTc0MTQ1NH0.RB9GWWniGrJcgntBi3HX35PLxx9Z8iw6o4txZv3Hh-WBta4UnYntJxjUbtcEBxC_etuiSzwWLVjp862wAyJV5j2KjaxzOT8aVwrtLc_vayDPwCXSruKmjqU9vWf0FRMoE0zzU3ts6P1J1_8DgbbBqC8H_rdXC_Zz2-MHWReA3FANAPjzrh3tdsiIvjGtB1oECXB-nl6LU68ucqUP_i5BnfQDCUSBWE4CLDv4F_qvvIQJIODxG6bEJ6LbBRVIwapBWTh-J2L639bO7CjXleJmpwzFoxvPBo-IxI3e7J9S5dROd9ylVxVBMDMjLqonpHyrYzPfa15xmsPLJEc0WzupUw");
    String webinSampleAccession2 =
        Objects.requireNonNull(webinSampleResource2.get(0)).getAccession();

    Optional<EntityModel<Sample>> webinSamplePostPersistence2 =
        this.publicClient.fetchSampleResource(
            webinSampleAccession2,
            "eyJhbGciOiJSUzI1NiJ9.eyJwcmluY2lwbGUiOiJXZWJpbi01NzE3NiIsInJvbGUiOlsiTUVUQUdFTk9NRV9BTkFMWVNJUyJdLCJleHAiOjE2NjU3NTk0NTQsImlhdCI6MTY2NTc0MTQ1NH0.RB9GWWniGrJcgntBi3HX35PLxx9Z8iw6o4txZv3Hh-WBta4UnYntJxjUbtcEBxC_etuiSzwWLVjp862wAyJV5j2KjaxzOT8aVwrtLc_vayDPwCXSruKmjqU9vWf0FRMoE0zzU3ts6P1J1_8DgbbBqC8H_rdXC_Zz2-MHWReA3FANAPjzrh3tdsiIvjGtB1oECXB-nl6LU68ucqUP_i5BnfQDCUSBWE4CLDv4F_qvvIQJIODxG6bEJ6LbBRVIwapBWTh-J2L639bO7CjXleJmpwzFoxvPBo-IxI3e7J9S5dROd9ylVxVBMDMjLqonpHyrYzPfa15xmsPLJEc0WzupUw");

    if (!webinSamplePostPersistence2.isPresent()) {
      throw new IntegrationTestFailException(
          "Private sample submitted using webin auth not retrieved", Phase.SIX);
    }*/

    final Sample webinSampleMinimalInfo = getWebinSampleMinimalInfo();
    final Map<String, String> sampleAccessionToNameMap =
        webinClient.bulkAccessionV2(Collections.singletonList(webinSampleMinimalInfo));

    if (sampleAccessionToNameMap.size() == 0) {
      throw new IntegrationTestFailException("Bulk accession is not working for V2", Phase.SIX);
    }

    // test private sample create and fetch using webin auth - v2
    try {
      final Sample webinTestSampleV2Submission = getWebinSampleTest1();
      final List<Sample> apiResponseSampleResourceList =
          webinClient.persistSampleResourceV2(
              Collections.singletonList(webinTestSampleV2Submission));
      final String apiResponseSampleAccession1 =
          Objects.requireNonNull(apiResponseSampleResourceList.get(0)).getAccession();
      final Attribute attributePre = Attribute.build("organism part", "lung");
      final Attribute attributePost = Attribute.build("organism part", "lungs");
      final Curation curation = Curation.build(attributePre, attributePost);

      webinClient.persistCuration(apiResponseSampleAccession1, curation, "Webin-40894", true);

      final Map<String, Sample> apiResponseV2SampleBulkFetch =
          webinClient.fetchSampleResourcesByAccessionsV2(
              Collections.singletonList(apiResponseSampleAccession1));

      if (apiResponseV2SampleBulkFetch.isEmpty()) {
        throw new IntegrationTestFailException(
            "Private sample submitted using webin auth using the V2 end point is not retrieved",
            Phase.SIX);
      } else {
        log.info("Found private sample by webin account");
        final Collection<Sample> foundSamples = apiResponseV2SampleBulkFetch.values();

        foundSamples.forEach(
            // TODO: only sample sample, remove forEach
            sample -> {
              log.info(String.valueOf(sample));

              final Optional<Attribute> curatedAttribute =
                  sample.getAttributes().stream()
                      .filter(attribute -> attribute.getType().equals("organism part"))
                      .findFirst();

              if (curatedAttribute.isPresent()) {
                final Attribute attribute = curatedAttribute.get();

                log.info("Curated attribute value " + attribute.getValue());

                if (attribute.getValue().equals("lungs")) {
                  throw new IntegrationTestFailException(
                      "Curated sample returned, uncurated expected", Phase.SIX);
                }
              }
            });
      }

      final Sample v2SingleSample = webinClient.fetchSampleResourceV2(apiResponseSampleAccession1);

      if (v2SingleSample == null) {
        throw new IntegrationTestFailException(
            "Private sample submitted using webin auth using the V2 end point is not retrieved using single sample retrieval endpoint",
            Phase.SIX);
      } else {
        log.info("Found private sample by webin account using single sample retrieval endpoint");

        log.info(String.valueOf(v2SingleSample));

        final Optional<Attribute> curatedAttribute =
            v2SingleSample.getAttributes().stream()
                .filter(attribute -> attribute.getType().equals("organism part"))
                .findFirst();

        if (curatedAttribute.isPresent()) {
          final Attribute attribute = curatedAttribute.get();

          log.info("Curated attribute value " + attribute.getValue());

          if (attribute.getValue().equals("lungs")) {
            throw new IntegrationTestFailException(
                "Curated sample returned in single sample retrieval endpoint, uncurated expected",
                Phase.SIX);
          }
        }
      }
    } catch (final Exception e) {
      throw new IntegrationTestFailException("V2 persist and fetch tests failed", Phase.SIX);
    }

    // multiple sample fetch by accessions test - v2, authorized user
    final Map<String, Sample> sampleResourcesV2Map1 =
        webinClient.fetchSampleResourcesByAccessionsV2(
            Arrays.asList(webinSampleAccession, "SAMEA1", "SAMEA8"));

    if (sampleResourcesV2Map1 == null || sampleResourcesV2Map1.size() == 0) {
      throw new IntegrationTestFailException("Multi sample fetch is not working - V2", Phase.SIX);
    }

    // single private sample fetch by accessions test - v2, authorized user
    final Sample fetchedSample = webinClient.fetchSampleResourceV2(webinSampleAccession);

    if (fetchedSample == null) {
      throw new IntegrationTestFailException(
          "Single private sample fetch is not working - V2, authorized user", Phase.SIX);
    }

    // multiple sample fetch by accessions test - v2, unauthorized user
    final Map<String, Sample> sampleResourcesV2Map2 =
        annonymousClient.fetchSampleResourcesByAccessionsV2(
            Arrays.asList(webinSampleAccession, "SAMEA1", "SAMEA14"));

    if (sampleResourcesV2Map2.size() > 2) {
      throw new IntegrationTestFailException(
          "Multi sample fetch is not working - V2, unauthorized user has access to private samples submitted by other submitters",
          Phase.SIX);
    }

    if (sampleResourcesV2Map2.size() < 2) {
      throw new IntegrationTestFailException(
          "Multi sample fetch is not working - V2, unauthorized user unable to fetch public samples",
          Phase.SIX);
    }

    // single private sample fetch by accessions test - v2, authorized user
    final Sample fetchedSample2 = annonymousClient.fetchSampleResourceV2(webinSampleAccession);

    if (fetchedSample2 != null) {
      throw new IntegrationTestFailException(
          "Single private sample fetch is not working - V2, fetching possible for unauthorized user",
          Phase.SIX);
    }

    // single public sample fetch by accessions test - v2, unauthorized user
    final Sample fetchedSample3 = webinClient.fetchSampleResourceV2("SAMEA8");

    if (fetchedSample3 == null) {
      throw new IntegrationTestFailException(
          "Single public sample fetch is not working - V2, fetching of public sample not working for unauthorized user",
          Phase.SIX);
    }

    // multiple sample fetch by accessions test, authorized user - v2, all samples not found,
    // partial
    // fetch result
    final Map<String, Sample> sampleResourcesV2Map3 =
        webinClient.fetchSampleResourcesByAccessionsV2(
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
    final String name = "RestIntegrationWebin_sample_1";
    final Instant update = Instant.parse("2016-05-05T11:36:57.00Z");
    final Instant release = Instant.parse("2116-04-01T11:36:57.00Z");

    final SortedSet<Attribute> attributes = new TreeSet<>();
    attributes.add(
        Attribute.build(
            "organism", "Homo sapiens", "http://purl.obolibrary.org/obo/NCBITaxon_9606", null));
    attributes.add(Attribute.build("age", "3", null, Collections.emptyList(), "year"));
    attributes.add(Attribute.build("organism part", "lung"));
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

  private Sample getWebinSampleMinimalInfo() {
    final String name = "RestIntegrationWebin_sample_1";

    return new Sample.Builder(name).build();
  }
}
