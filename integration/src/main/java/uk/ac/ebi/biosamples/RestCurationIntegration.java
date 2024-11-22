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

import java.net.URI;
import java.time.Instant;
import java.util.*;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.annotation.Order;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestOperations;
import org.springframework.web.util.UriComponentsBuilder;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.client.utils.ClientProperties;
import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.Curation;
import uk.ac.ebi.biosamples.model.Relationship;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.utils.IntegrationTestFailException;

@Component
@Order(6)
// @Profile({ "default", "rest" })
public class RestCurationIntegration extends AbstractIntegration {
  private final Logger log = LoggerFactory.getLogger(getClass());
  private final ClientProperties clientProperties;
  private final RestOperations restTemplate;

  public RestCurationIntegration(
      final RestTemplateBuilder restTemplateBuilder,
      final ClientProperties clientProperties,
      final BioSamplesClient client) {
    super(client);

    restTemplate = restTemplateBuilder.build();
    this.clientProperties = clientProperties;
  }

  @Override
  protected void phaseOne() {
    client.persistSampleResource(getSampleTest1());
    client.persistSampleResource(getSampleTest2());
    client.persistSampleResource(getSampleTest3());
  }

  @Override
  protected void phaseTwo() {
    Sample sample = getSampleTest1();
    Optional<Sample> optionalSample = fetchUniqueSampleByName(sample.getName());

    if (optionalSample.isPresent()) {
      sample = optionalSample.get();
    } else {
      throw new IntegrationTestFailException(
          "Sample does not exist, sample name: " + sample.getName(), Phase.TWO);
    }

    Sample sample2 = getSampleTest2();
    optionalSample = fetchUniqueSampleByName(sample2.getName());

    if (optionalSample.isPresent()) {
      sample2 = optionalSample.get();
    } else {
      throw new IntegrationTestFailException(
          "Sample does not exist, sample name: " + sample2.getName(), Phase.TWO);
    }

    Sample sample3 = getSampleTest3();
    optionalSample = fetchUniqueSampleByName(sample3.getName());

    if (optionalSample.isPresent()) {
      sample3 = optionalSample.get();
    } else {
      throw new IntegrationTestFailException(
          "Sample does not exist, sample name: " + sample3.getName(), Phase.TWO);
    }

    // resubmit sample with relationships
    final SortedSet<Relationship> relationships = new TreeSet<>();
    relationships.add(
        Relationship.build(sample3.getAccession(), "DERIVED_FROM", sample.getAccession()));
    sample3 = Sample.Builder.fromSample(sample3).withRelationships(relationships).build();

    client.persistSampleResource(sample3);

    Set<Attribute> attributesPre;
    Set<Attribute> attributesPost;

    attributesPre = new HashSet<>();
    attributesPre.add(Attribute.build("Organism", "9606"));
    attributesPost = new HashSet<>();
    attributesPost.add(Attribute.build("Organism", "Homo sapiens"));

    client.persistCuration(
        sample.getAccession(),
        Curation.build(attributesPre, attributesPost, null, null),
        clientProperties.getBiosamplesClientWebinUsername());

    attributesPre = new HashSet<>();
    attributesPre.add(Attribute.build("Organism", "Homo sapiens"));
    attributesPost = new HashSet<>();
    attributesPost.add(
        Attribute.build(
            "Organism", "Homo sapiens", "http://purl.obolibrary.org/obo/NCBITaxon_9606", null));

    client.persistCuration(
        sample.getAccession(),
        Curation.build(attributesPre, attributesPost, null, null),
        clientProperties.getBiosamplesClientWebinUsername());

    attributesPre = new HashSet<>();
    attributesPre.add(Attribute.build("Weird", "\"\""));
    attributesPost = new HashSet<>();

    client.persistCuration(
        sample.getAccession(),
        Curation.build(attributesPre, attributesPost, null, null),
        clientProperties.getBiosamplesClientWebinUsername());

    // test alternative domain interpretations
    attributesPre = new HashSet<>();
    attributesPre.add(Attribute.build("CurationDomain", "original"));
    attributesPost = new HashSet<>();
    attributesPost.add(Attribute.build("CurationDomain", "A"));

    client.persistCuration(
        sample.getAccession(),
        Curation.build(attributesPre, attributesPost, null, null),
        clientProperties.getBiosamplesClientWebinUsername());

    attributesPre = new HashSet<>();
    attributesPre.add(Attribute.build("CurationDomain", "original"));
    attributesPost = new HashSet<>();
    attributesPost.add(Attribute.build("CurationDomain", "B"));

    client.persistCuration(
        sample.getAccession(),
        Curation.build(attributesPre, attributesPost, null, null),
        clientProperties.getBiosamplesClientWebinUsername());

    final Set<Relationship> relationshipsPre = new HashSet<>();
    final Set<Relationship> relationshipsPost = new HashSet<>();

    relationshipsPost.add(
        Relationship.build(sample.getAccession(), "SAME_AS", sample2.getAccession()));

    client.persistCuration(
        sample.getAccession(),
        Curation.build(null, null, null, null, relationshipsPre, relationshipsPost),
        clientProperties.getBiosamplesClientWebinUsername());
  }

  @Override
  protected void phaseThree() {
    Sample sample = getSampleTest1();
    final Optional<Sample> optionalSample = fetchUniqueSampleByName(sample.getName());

    if (optionalSample.isPresent()) {
      sample =
          Sample.Builder.fromSample(sample)
              .withAccession(optionalSample.get().getAccession())
              .build();
    } else {
      throw new IntegrationTestFailException(
          "Sample does not exist, sample name: " + sample.getName(), Phase.TWO);
    }

    // check /curations
    testCurations();
    testSampleCurations(sample);

    // check there were no side-effects
    client.fetchSampleResource(sample.getAccession());

    // check what the default alldomain conflicting result is
    MultiValueMap<String, String> params;
    params = new LinkedMultiValueMap<>();

    testSampleCurationDomains(sample.getAccession(), "A", params);
    // check what the no-domain result is
    params = new LinkedMultiValueMap<>();
    params.add("curationdomain", "");

    testSampleCurationDomains(sample.getAccession(), "original", params);
    // check what a single-domain result is
    params = new LinkedMultiValueMap<>();
    params.add("curationdomain", "self.BiosampleIntegrationTest");

    testSampleCurationDomains(sample.getAccession(), "A", params);
    params = new LinkedMultiValueMap<>();
    params.add("curationdomain", "self.BiosampleIntegrationTestAlternative");

    testSampleCurationDomains(sample.getAccession(), "B", params);
  }

  @Override
  protected void phaseFour() {
    Sample sample3 = getSampleTest3();
    final Optional<Sample> optionalSample = fetchUniqueSampleByName(sample3.getName());

    if (optionalSample.isPresent()) {
      sample3 = optionalSample.get();
    } else {
      throw new IntegrationTestFailException(
          "Sample does not exist, sample name: " + sample3.getName(), Phase.TWO);
    }

    final Set<Relationship> relationshipsPre = new HashSet<>();

    relationshipsPre.add(sample3.getRelationships().first());

    final Set<Relationship> relationshipsPost = new HashSet<>();

    client.persistCuration(
        sample3.getAccession(),
        Curation.build(null, null, null, null, relationshipsPre, relationshipsPost),
        clientProperties.getBiosamplesClientWebinUsername());
  }

  @Override
  protected void phaseFive() {
    Sample sample3 = getSampleTest3();
    final Optional<Sample> optionalSample = fetchUniqueSampleByName(sample3.getName());

    if (optionalSample.isPresent()) {
      sample3 = optionalSample.get();
    } else {
      throw new IntegrationTestFailException(
          "Sample does not exist, sample name: " + sample3.getName(), Phase.TWO);
    }

    Assert.assertTrue(sample3.getRelationships().isEmpty());
  }

  @Override
  protected void phaseSix() {}

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
    for (final EntityModel<Curation> curationResource : client.fetchCurationResourceAll()) {
      final Link selfLink = curationResource.getLink("self").get();
      final Link samplesLink = curationResource.getLink("samples").get();

      URI uriLink = URI.create(selfLink.getHref());

      log.info("GETting from " + uriLink);

      RequestEntity<Void> requestLink =
          RequestEntity.get(uriLink).accept(MediaTypes.HAL_JSON).build();
      final ResponseEntity<EntityModel<Curation>> responseLink =
          restTemplate.exchange(requestLink, new ParameterizedTypeReference<>() {});

      if (!responseLink.getStatusCode().is2xxSuccessful()) {
        throw new RuntimeException("Unable to follow self link on " + curationResource);
      }

      log.info("GETted from " + uriLink);

      uriLink = URI.create(samplesLink.getHref());

      log.info("GETting from " + uriLink);

      requestLink = RequestEntity.get(uriLink).accept(MediaTypes.HAL_JSON).build();

      final ResponseEntity<PagedModel<EntityModel<Sample>>> responseLink2 =
          restTemplate.exchange(requestLink, new ParameterizedTypeReference<>() {});

      if (!responseLink2.getStatusCode().is2xxSuccessful()) {
        throw new RuntimeException("Unable to follow samples link on " + curationResource);
      }

      log.info("GETted from " + uriLink);
    }
  }

  private void testSampleCurations(final Sample sample) {
    // TODO use client
    final URI uri =
        UriComponentsBuilder.fromUri(clientProperties.getBiosamplesClientUri())
            .pathSegment("samples")
            .pathSegment(sample.getAccession())
            .pathSegment("curationlinks")
            .build()
            .toUri();

    log.info("GETting from " + uri);

    final RequestEntity<Void> request = RequestEntity.get(uri).accept(MediaTypes.HAL_JSON).build();
    final ResponseEntity<PagedModel<EntityModel<Curation>>> response =
        restTemplate.exchange(request, new ParameterizedTypeReference<>() {});

    final PagedModel<EntityModel<Curation>> paged = response.getBody();

    if (Objects.requireNonNull(paged.getMetadata()).getTotalElements() != 6) {
      throw new RuntimeException(
          "Expecting 6 curations, found " + paged.getMetadata().getTotalElements());
    }
  }

  private void testSampleCurationDomains(
      final String accession, final String expected, final MultiValueMap<String, String> params) {
    // TODO use client
    final URI uri =
        UriComponentsBuilder.fromUri(clientProperties.getBiosamplesClientUri())
            .pathSegment("samples")
            .pathSegment(accession)
            .queryParams(params)
            .build()
            .toUri();

    log.info("GETting from " + uri);
    final RequestEntity<Void> request = RequestEntity.get(uri).accept(MediaTypes.HAL_JSON).build();
    final ResponseEntity<EntityModel<Sample>> response =
        restTemplate.exchange(request, new ParameterizedTypeReference<>() {});
    final EntityModel<Sample> paged = response.getBody();

    for (final Attribute attribute : paged.getContent().getAttributes()) {
      if ("CurationDomain".equals(attribute.getType())) {
        if (!expected.equals(attribute.getValue())) {
          throw new RuntimeException("Expecting " + expected + ", found " + attribute.getValue());
        }
      }
    }
  }

  private Sample getSampleTest1() {
    final String name = "RestCurationIntegration_sample_1";
    final Instant update = Instant.parse("2016-05-05T11:36:57.00Z");
    final Instant release = Instant.parse("2016-04-01T11:36:57.00Z");

    final SortedSet<Attribute> attributes = new TreeSet<>();
    attributes.add(Attribute.build("Organism", "9606"));
    attributes.add(Attribute.build("CurationDomain", "original"));
    attributes.add(Attribute.build("Weird", "\"\""));

    return new Sample.Builder(name)
        .withWebinSubmissionAccountId(clientProperties.getBiosamplesClientWebinUsername())
        .withRelease(release)
        .withUpdate(update)
        .withAttributes(attributes)
        .build();
  }

  private Sample getSampleTest2() {
    final String name = "RestCurationIntegration_sample_2";
    final Instant update = Instant.parse("2016-05-05T11:36:57.00Z");
    final Instant release = Instant.parse("2016-04-01T11:36:57.00Z");

    final SortedSet<Attribute> attributes = new TreeSet<>();
    attributes.add(Attribute.build("Organism", "9606"));
    attributes.add(Attribute.build("CurationDomain", "original"));
    attributes.add(Attribute.build("Weird", "\"\""));

    return new Sample.Builder(name)
        .withWebinSubmissionAccountId(clientProperties.getBiosamplesClientWebinUsername())
        .withRelease(release)
        .withUpdate(update)
        .withAttributes(attributes)
        .build();
  }

  private Sample getSampleTest3() {
    final String name = "RestCurationIntegration_sample_3";
    final Instant update = Instant.parse("2016-05-05T11:36:57.00Z");
    final Instant release = Instant.parse("2016-04-01T11:36:57.00Z");

    final SortedSet<Attribute> attributes = new TreeSet<>();
    attributes.add(Attribute.build("Organism", "9606"));

    return new Sample.Builder(name)
        .withWebinSubmissionAccountId(clientProperties.getBiosamplesClientWebinUsername())
        .withRelease(release)
        .withUpdate(update)
        .withAttributes(attributes)
        .build();
  }
}
