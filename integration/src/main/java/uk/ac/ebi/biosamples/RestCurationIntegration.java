/*
* Copyright 2019 EMBL - European Bioinformatics Institute
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.annotation.Order;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestOperations;
import org.springframework.web.util.UriComponentsBuilder;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.*;
import uk.ac.ebi.biosamples.utils.IntegrationTestFailException;

@Component
@Order(6)
// @Profile({ "default", "rest" })
public class RestCurationIntegration extends AbstractIntegration {

  private Logger log = LoggerFactory.getLogger(this.getClass());

  private final IntegrationProperties integrationProperties;
  private final BioSamplesProperties bioSamplesProperties;
  private final RestOperations restTemplate;

  public RestCurationIntegration(
      RestTemplateBuilder restTemplateBuilder,
      IntegrationProperties integrationProperties,
      BioSamplesProperties bioSamplesProperties,
      BioSamplesClient client) {
    super(client);
    this.restTemplate = restTemplateBuilder.build();
    this.integrationProperties = integrationProperties;
    this.bioSamplesProperties = bioSamplesProperties;
  }

  @Override
  protected void phaseOne() {
    Sample sample = getSampleTest1();
    client.persistSampleResource(sample);
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

    Set<Attribute> attributesPre;
    Set<Attribute> attributesPost;

    attributesPre = new HashSet<>();
    attributesPre.add(Attribute.build("Organism", "9606"));
    attributesPost = new HashSet<>();
    attributesPost.add(Attribute.build("Organism", "Homo sapiens"));
    client.persistCuration(
        sample.getAccession(),
        Curation.build(attributesPre, attributesPost, null, null),
        "self.BiosampleIntegrationTest");

    attributesPre = new HashSet<>();
    attributesPre.add(Attribute.build("Organism", "Homo sapiens"));
    attributesPost = new HashSet<>();
    attributesPost.add(
        Attribute.build(
            "Organism", "Homo sapiens", "http://purl.obolibrary.org/obo/NCBITaxon_9606", null));
    client.persistCuration(
        sample.getAccession(),
        Curation.build(attributesPre, attributesPost, null, null),
        "self.BiosampleIntegrationTest");

    attributesPre = new HashSet<>();
    attributesPre.add(Attribute.build("Weird", "\"\""));
    attributesPost = new HashSet<>();
    client.persistCuration(
        sample.getAccession(),
        Curation.build(attributesPre, attributesPost, null, null),
        "self.BiosampleIntegrationTest");

    // test alternative domain interpretations
    attributesPre = new HashSet<>();
    attributesPre.add(Attribute.build("CurationDomain", "original"));
    attributesPost = new HashSet<>();
    attributesPost.add(Attribute.build("CurationDomain", "A"));
    client.persistCuration(
        sample.getAccession(),
        Curation.build(attributesPre, attributesPost, null, null),
        "self.BiosampleIntegrationTest");
    attributesPre = new HashSet<>();
    attributesPre.add(Attribute.build("CurationDomain", "original"));
    attributesPost = new HashSet<>();
    attributesPost.add(Attribute.build("CurationDomain", "B"));
    client.persistCuration(
        sample.getAccession(),
        Curation.build(attributesPre, attributesPost, null, null),
        "self.BiosampleIntegrationTestAlternative");
  }

  @Override
  protected void phaseThree() {
    Sample sample = getSampleTest1();
    Optional<Sample> optionalSample = fetchUniqueSampleByName(sample.getName());
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

    // check there was no side-effects
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
    // TODO Auto-generated method stub

  }

  @Override
  protected void phaseFive() {
    // TODO Auto-generated method stub

  }

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
    for (Resource<Curation> curationResource : client.fetchCurationResourceAll()) {
      Link selfLink = curationResource.getLink("self");
      Link samplesLink = curationResource.getLink("samples");

      if (selfLink == null) {
        throw new RuntimeException("Must have self link on " + curationResource);
      } else {
        URI uriLink = URI.create(selfLink.getHref());
        log.info("GETting from " + uriLink);
        RequestEntity<Void> requestLink =
            RequestEntity.get(uriLink).accept(MediaTypes.HAL_JSON).build();
        ResponseEntity<Resource<Curation>> responseLink =
            restTemplate.exchange(
                requestLink, new ParameterizedTypeReference<Resource<Curation>>() {});
        if (!responseLink.getStatusCode().is2xxSuccessful()) {
          throw new RuntimeException("Unable to follow self link on " + curationResource);
        }
        log.info("GETted from " + uriLink);
      }

      if (samplesLink == null) {
        throw new RuntimeException("Must have samples link on " + curationResource);
      } else {
        URI uriLink = URI.create(samplesLink.getHref());
        log.info("GETting from " + uriLink);
        RequestEntity<Void> requestLink =
            RequestEntity.get(uriLink).accept(MediaTypes.HAL_JSON).build();
        ResponseEntity<PagedResources<Resource<Sample>>> responseLink =
            restTemplate.exchange(
                requestLink, new ParameterizedTypeReference<PagedResources<Resource<Sample>>>() {});
        if (!responseLink.getStatusCode().is2xxSuccessful()) {
          throw new RuntimeException("Unable to follow samples link on " + curationResource);
        }
        log.info("GETted from " + uriLink);
      }
    }
  }

  private void testSampleCurations(Sample sample) {
    // TODO use client
    URI uri =
        UriComponentsBuilder.fromUri(bioSamplesProperties.getBiosamplesClientUri())
            .pathSegment("samples")
            .pathSegment(sample.getAccession())
            .pathSegment("curationlinks")
            .build()
            .toUri();

    log.info("GETting from " + uri);
    RequestEntity<Void> request = RequestEntity.get(uri).accept(MediaTypes.HAL_JSON).build();
    ResponseEntity<PagedResources<Resource<Curation>>> response =
        restTemplate.exchange(
            request, new ParameterizedTypeReference<PagedResources<Resource<Curation>>>() {});

    PagedResources<Resource<Curation>> paged = response.getBody();

    if (paged.getMetadata().getTotalElements() != 5) {
      throw new RuntimeException(
          "Expecting 4 curations, found " + paged.getMetadata().getTotalElements());
    }
  }

  private void testSampleCurationDomains(
      String accession, String expected, MultiValueMap<String, String> params) {
    // TODO use client
    URI uri =
        UriComponentsBuilder.fromUri(bioSamplesProperties.getBiosamplesClientUri())
            .pathSegment("samples")
            .pathSegment(accession)
            .queryParams(params)
            .build()
            .toUri();

    log.info("GETting from " + uri);
    RequestEntity<Void> request = RequestEntity.get(uri).accept(MediaTypes.HAL_JSON).build();
    ResponseEntity<Resource<Sample>> response =
        restTemplate.exchange(request, new ParameterizedTypeReference<Resource<Sample>>() {});

    Resource<Sample> paged = response.getBody();

    for (Attribute attribute : paged.getContent().getAttributes()) {
      if ("CurationDomain".equals(attribute.getType())) {
        if (!expected.equals(attribute.getValue())) {
          throw new RuntimeException("Expecting " + expected + ", found " + attribute.getValue());
        }
      }
    }
  }

  private void testSampleCurationDomains(
      String accession, String expected, Optional<List<String>> curationDomains) {
    Optional<Resource<Sample>> sample = client.fetchSampleResource(accession, curationDomains);
    for (Attribute attribute : sample.get().getContent().getAttributes()) {
      if ("CurationDomain".equals(attribute.getType())) {
        if (!expected.equals(attribute.getValue())) {
          throw new RuntimeException("Expecting " + expected + ", found " + attribute.getValue());
        }
      }
    }
  }

  private Sample getSampleTest1() {
    String name = "RestCurationIntegration_sample_1";
    Instant update = Instant.parse("2016-05-05T11:36:57.00Z");
    Instant release = Instant.parse("2016-04-01T11:36:57.00Z");

    SortedSet<Attribute> attributes = new TreeSet<>();
    attributes.add(Attribute.build("Organism", "9606"));
    attributes.add(Attribute.build("CurationDomain", "original"));
    attributes.add(Attribute.build("Weird", "\"\""));

    SortedSet<Relationship> relationships = new TreeSet<>();
    SortedSet<ExternalReference> externalReferences = new TreeSet<>();

    return new Sample.Builder(name)
        .withDomain(defaultIntegrationSubmissionDomain)
        .withRelease(release)
        .withUpdate(update)
        .withAttributes(attributes)
        .withRelationships(relationships)
        .withExternalReferences(externalReferences)
        .build();
  }
}
