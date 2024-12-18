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
import org.springframework.hateoas.mediatype.hal.Jackson2HalModule;
import org.springframework.hateoas.server.mvc.TypeConstrainedMappingJackson2HttpMessageConverter;
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
import uk.ac.ebi.biosamples.client.utils.ClientProperties;
import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.Relationship;
import uk.ac.ebi.biosamples.model.Sample;

@Component
@Profile({"big"})
public class BigIntegration extends AbstractIntegration {
  private static final int timeout = 100000;
  private final Logger log = LoggerFactory.getLogger(getClass());
  private final RestOperations restOperations;
  private final ClientProperties clientProperties;
  // must be over 1000
  private final int firstInteger = 10000000;
  private final int noSamples = 5000;

  public BigIntegration(
      final BioSamplesClient client,
      final RestTemplateBuilder restTemplateBuilder,
      final ClientProperties clientProperties) {
    super(client);
    final RestTemplate restTemplate = restTemplateBuilder.build();

    // make sure there is a application/hal+json converter
    // traverson will make its own but not if we want to customize the resttemplate in any way
    // (e.g.
    // caching)
    final List<HttpMessageConverter<?>> converters = restTemplate.getMessageConverters();
    final ObjectMapper mapper = new ObjectMapper();

    mapper.registerModule(new Jackson2HalModule());
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    final MappingJackson2HttpMessageConverter halConverter =
        new TypeConstrainedMappingJackson2HttpMessageConverter(RepresentationModel.class);

    halConverter.setObjectMapper(mapper);
    halConverter.setSupportedMediaTypes(Collections.singletonList(MediaTypes.HAL_JSON));
    // make sure this is inserted first
    converters.add(0, halConverter);
    restTemplate.setMessageConverters(converters);

    restOperations = restTemplate;

    this.clientProperties = clientProperties;
  }

  @Override
  protected void phaseOne() {
    final List<Sample> samples = new ArrayList<>();
    // generate a root sample
    final Sample root = generateSample(firstInteger, Collections.emptyList(), null);

    samples.add(root);
    // generate a large number of samples
    for (int i = 1; i < noSamples; i++) {
      final Sample sample = generateSample(firstInteger + i, Collections.emptyList(), root);

      samples.add(sample);
    }
    // generate one sample to rule them all
    samples.add(generateSample(firstInteger + noSamples, samples, null));

    // time how long it takes to post them

    final long startTime = System.nanoTime();

    webinClient.persistSamples(samples);

    final long endTime = System.nanoTime();
    final double elapsedMs = (int) ((endTime - startTime) / 1000000L);
    final double msPerSample = elapsedMs / noSamples;

    log.info(
        "Submitted " + noSamples + " samples in " + elapsedMs + "ms (" + msPerSample + "ms each)");

    if (msPerSample > 100) {
      throw new RuntimeException(
          "Took more than 100ms per sample to post (" + msPerSample + "ms each)");
    }
  }

  @Override
  protected void phaseTwo() {
    long startTime;
    long endTime;
    double elapsedMs;
    // time how long it takes to get the highly connected sample
    startTime = System.nanoTime();

    webinClient.fetchSampleResource("SAMEA" + (firstInteger + noSamples));

    endTime = System.nanoTime();
    elapsedMs = (int) ((endTime - startTime) / 1000000L);

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

    webinClient.fetchSampleResource("SAMEA" + firstInteger);

    endTime = System.nanoTime();
    elapsedMs = (int) ((endTime - startTime) / 1000000L);

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

    webinClient.fetchSampleResourceAll(); // do nothing

    endTime = System.nanoTime();
    elapsedMs = (int) ((endTime - startTime) / 1000000L);

    if (elapsedMs > timeout) {
      throw new RuntimeException(
          "Took more than  " + timeout + "ms to fetch all samples (" + elapsedMs + "ms)");
    }

    log.info("Took " + elapsedMs + "ms to fetch all samples");

    // TODO check HAL links for search term and facets are persistent over paging etc

    final URI uri =
        UriComponentsBuilder.fromUri(clientProperties.getBiosamplesClientUri())
            .pathSegment("samples")
            .queryParam("text", "Sample")
            .queryParam("filter", "attr:organism:Homo sapiens")
            .build()
            .encode()
            .toUri();
    log.info("checking HAL links on " + uri);

    final ResponseEntity<PagedModel<EntityModel<Sample>>> responseEntity =
        restOperations.exchange(
            RequestEntity.get(uri).accept(MediaTypes.HAL_JSON).build(),
            new ParameterizedTypeReference<PagedModel<EntityModel<Sample>>>() {});
    final PagedModel<EntityModel<Sample>> page = responseEntity.getBody();

    log.info("looking for links in " + page);

    for (final Link link : page.getLinks()) {
      log.info("Found link " + link);
    }

    final Link firstLink = page.getLink(IanaLinkRelations.FIRST).get();
    final UriComponents firstLinkUriComponents =
        UriComponentsBuilder.fromUriString(firstLink.getHref()).build();
    final String firstFilter = firstLinkUriComponents.getQueryParams().get("filter").get(0);

    if (!"attr:organism:Homo%20sapiens".equals(firstFilter)) {
      throw new RuntimeException(
          "Expected first relationship URL to include parameter filter with value 'attr:organism:Homo sapiens' but got '"
              + firstFilter
              + "'");
    }

    final String firstText = firstLinkUriComponents.getQueryParams().get("text").get(0);

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

  @Override
  protected void phaseSix() {}

  private Sample generateSample(final int i, final List<Sample> samples, final Sample root) {
    final Instant update = Instant.parse("2016-05-05T11:36:57.00Z");
    final Instant release = Instant.parse("2016-04-01T11:36:57.00Z");
    final SortedSet<Attribute> attributes = new TreeSet<>();
    final SortedSet<Relationship> relationships = new TreeSet<>();

    attributes.add(
        Attribute.build(
            "organism",
            "Homo sapiens",
            null,
            Lists.newArrayList("http://purl.obolibrary.org/obo/NCBITaxon_9606"),
            null));

    for (final Sample other : samples) {
      relationships.add(Relationship.build("SAMEA" + i, "derived from", other.getAccession()));
    }

    if (root != null) {
      relationships.add(Relationship.build("SAMEA" + i, "derived from", root.getAccession()));
    }

    final Sample sample =
        new Sample.Builder("big sample " + i, "SAMEA" + i)
            .withWebinSubmissionAccountId(clientProperties.getBiosamplesClientWebinUsername())
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
