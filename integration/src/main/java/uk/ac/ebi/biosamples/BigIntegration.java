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

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import java.net.URI;
import java.time.Instant;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Profile;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.*;
import org.springframework.hateoas.hal.Jackson2HalModule;
import org.springframework.hateoas.mvc.TypeConstrainedMappingJackson2HttpMessageConverter;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.Relationship;
import uk.ac.ebi.biosamples.model.Sample;

@Component
@Profile({"big"})
public class BigIntegration extends AbstractIntegration {

  private Logger log = LoggerFactory.getLogger(this.getClass());
  private final RestOperations restOperations;
  private final BioSamplesProperties bioSamplesProperties;

  // must be over 1000
  private final int firstInteger = 10000000;
  private final int noSamples = 5000;

  private static final int timeout = 100000;

  public BigIntegration(
      BioSamplesClient client,
      RestTemplateBuilder restTemplateBuilder,
      BioSamplesProperties bioSamplesProperties) {
    super(client);
    RestTemplate restTemplate = restTemplateBuilder.build();

    // make sure there is a application/hal+json converter
    // traverson will make its own but not if we want to customize the resttemplate in any way
    // (e.g.
    // caching)
    List<HttpMessageConverter<?>> converters = restTemplate.getMessageConverters();
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new Jackson2HalModule());
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    MappingJackson2HttpMessageConverter halConverter =
        new TypeConstrainedMappingJackson2HttpMessageConverter(ResourceSupport.class);
    halConverter.setObjectMapper(mapper);
    halConverter.setSupportedMediaTypes(Arrays.asList(MediaTypes.HAL_JSON));
    // make sure this is inserted first
    converters.add(0, halConverter);
    restTemplate.setMessageConverters(converters);

    this.restOperations = restTemplate;

    this.bioSamplesProperties = bioSamplesProperties;
  }

  @Override
  protected void phaseOne() {
    List<Sample> samples = new ArrayList<>();

    // generate a root sample
    Sample root = generateSample(firstInteger, Collections.emptyList(), null);
    samples.add(root);
    // generate a large number of samples
    for (int i = 1; i < noSamples; i++) {

      Sample sample = generateSample(firstInteger + i, Collections.emptyList(), root);
      samples.add(sample);
    }
    // generate one sample to rule them all
    samples.add(generateSample(firstInteger + noSamples, samples, null));

    // time how long it takes to submit them

    long startTime = System.nanoTime();
    client.persistSamples(samples);
    long endTime = System.nanoTime();

    double elapsedMs = (int) ((endTime - startTime) / 1000000l);
    double msPerSample = elapsedMs / noSamples;
    log.info(
        "Submitted " + noSamples + " samples in " + elapsedMs + "ms (" + msPerSample + "ms each)");
    if (msPerSample > 100) {
      throw new RuntimeException(
          "Took more than 100ms per sample to submit (" + msPerSample + "ms each)");
    }
  }

  @Override
  protected void phaseTwo() {
    long startTime;
    long endTime;
    double elapsedMs;

    // time how long it takes to get the highly connected sample

    startTime = System.nanoTime();
    client.fetchSample("SAMEA" + (firstInteger + noSamples));
    endTime = System.nanoTime();
    elapsedMs = (int) ((endTime - startTime) / 1000000l);
    if (elapsedMs > timeout) {
      throw new RuntimeException(
          "Took more than "
              + timeout
              + "ms to fetch highly-connected sample ("
              + elapsedMs
              + "ms)");
    }
    log.info("Took " + elapsedMs + "ms to fetch highly-connected sample SAMbig" + noSamples);

    startTime = System.nanoTime();
    client.fetchSample("SAMEA" + firstInteger);
    endTime = System.nanoTime();
    elapsedMs = (int) ((endTime - startTime) / 1000000l);
    if (elapsedMs > timeout) {
      throw new RuntimeException(
          "Took more than  "
              + timeout
              + "ms to fetch highly-connected sample ("
              + elapsedMs
              + "ms)");
    }
    log.info("Took " + elapsedMs + "ms to fetch highly-connected sample SAMbig0");

    // time how long it takes to loop over all of them

    startTime = System.nanoTime();
    for (Resource<Sample> sample : client.fetchSampleResourceAll()) {
      // do nothing
    }
    endTime = System.nanoTime();
    elapsedMs = (int) ((endTime - startTime) / 1000000l);
    if (elapsedMs > timeout) {
      throw new RuntimeException(
          "Took more than  " + timeout + "ms to fetch all samples (" + elapsedMs + "ms)");
    }
    log.info("Took " + elapsedMs + "ms to fetch all samples");

    // TODO check HAL links for search term and facets are persistent over paging etc

    URI uri =
        UriComponentsBuilder.fromUri(bioSamplesProperties.getBiosamplesClientUri())
            .pathSegment("samples")
            .queryParam("text", "Sample")
            .queryParam("filter", "attr:organism:Homo sapiens")
            .build()
            .encode()
            .toUri();
    log.info("checking HAL links on " + uri);
    ResponseEntity<PagedResources<Resource<Sample>>> responseEntity =
        restOperations.exchange(
            RequestEntity.get(uri).accept(MediaTypes.HAL_JSON).build(),
            new ParameterizedTypeReference<PagedResources<Resource<Sample>>>() {});

    PagedResources<Resource<Sample>> page = responseEntity.getBody();
    log.info("looking for links in " + page);
    for (Link link : page.getLinks()) {
      log.info("Found link " + link);
    }
    Link firstLink = page.getLink(Link.REL_FIRST);
    UriComponents firstLinkUriComponents =
        UriComponentsBuilder.fromUriString(firstLink.getHref()).build();

    String firstFilter = firstLinkUriComponents.getQueryParams().get("filter").get(0);
    if (!"attr:organism:Homo%20sapiens".equals(firstFilter)) {
      throw new RuntimeException(
          "Expected first relationship URL to include parameter filter with value 'attr:organism:Homo sapiens' but got '"
              + firstFilter
              + "'");
    }
    String firstText = firstLinkUriComponents.getQueryParams().get("text").get(0);
    if (!"Sample".equals(firstText)) {
      throw new RuntimeException(
          "Expected first relationship URL to include parameter text with value 'Sample' but got '"
              + firstText
              + "'");
    }
  }

  @Override
  protected void phaseThree() {
    // TODO Auto-generated method stub

  }

  @Override
  protected void phaseFour() {
    // TODO Auto-generated method stub

  }

  @Override
  protected void phaseFive() {
    // TODO Auto-generated method stub

  }

  public Sample generateSample(int i, List<Sample> samples, Sample root) {

    Instant update = Instant.parse("2016-05-05T11:36:57.00Z");
    Instant release = Instant.parse("2016-04-01T11:36:57.00Z");
    String domain = "self.BiosampleIntegrationTest";

    SortedSet<Attribute> attributes = new TreeSet<>();
    attributes.add(
        Attribute.build(
            "organism",
            "Homo sapiens",
            null,
            Lists.newArrayList("http://purl.obolibrary.org/obo/NCBITaxon_9606"),
            null));

    SortedSet<Relationship> relationships = new TreeSet<>();
    for (Sample other : samples) {
      relationships.add(Relationship.build("SAMEA" + i, "derived from", other.getAccession()));
    }
    if (root != null) {
      relationships.add(Relationship.build("SAMEA" + i, "derived from", root.getAccession()));
    }

    Sample sample =
        new Sample.Builder("big sample " + i, "SAMEA" + i)
            .withDomain(domain)
            .withRelease(release)
            .withUpdate(update)
            .withAttributes(attributes)
            .withRelationships(relationships)
            .build();

    //		Sample.build("big sample "+i, "SAMbig"+i, domain, release, update, attributes,
    // relationships, null, null, null, null);

    log.trace("built " + sample.getAccession());
    return sample;
  }
}
