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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import java.net.URI;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import org.hamcrest.CoreMatchers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.Sample;

@Component
// @Order(1)
// @Profile({"default", "rest"})
public class PhenopacketIntegration extends AbstractIntegration {

  private Logger log = LoggerFactory.getLogger(this.getClass());
  private final RestTemplate restTemplate;
  private BioSamplesProperties clientProperties;
  private final ObjectMapper mapper;

  public PhenopacketIntegration(
      BioSamplesClient client,
      RestTemplateBuilder restTemplateBuilder,
      BioSamplesProperties clientProperties) {
    super(client);
    this.restTemplate = restTemplateBuilder.build();
    this.clientProperties = clientProperties;
    this.mapper = new ObjectMapper();
  }

  @Override
  protected void phaseOne() {
    Sample testSample = getTestSample();
    Optional<EntityModel<Sample>> optionalSample =
        client.fetchSampleResource(testSample.getAccession());
    if (optionalSample.isPresent()) {
      throw new RuntimeException("Phenopacket test sample should not be available during phase 1");
    }
    EntityModel<Sample> resource = client.persistSampleResource(testSample);
    if (!testSample.equals(resource.getContent())) {
      throw new RuntimeException(
          "Expected response ("
              + resource.getContent()
              + ") to equal submission ("
              + testSample
              + ")");
    }
  }

  @Override
  protected void phaseTwo() {
    this.checkSampleWithOrphanetLinkWorks();
  }

  private void checkSampleWithOrphanetLinkWorks() {
    Sample testSample = getTestSample();

    Optional<EntityModel<Sample>> sampleResource =
        client.fetchSampleResource(testSample.getAccession());

    assertThat(sampleResource.isPresent(), CoreMatchers.is(true));
    URI sampleURI = URI.create(sampleResource.get().getLink("self").get().getHref());

    MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
    headers.add("Accept", "application/phenopacket+json");
    RequestEntity request = new RequestEntity<>(headers, HttpMethod.GET, sampleURI);
    ResponseEntity<String> response = restTemplate.exchange(request, String.class);
    if (!response.getStatusCode().is2xxSuccessful()) {
      throw new RuntimeException(
          "Impossible to retrieve correctly phenopacket sample with name " + testSample.getName());
    }

    List<LinkedHashMap> allMetadata =
        JsonPath.read(response.getBody(), "$.metaData.resources[?(@.id==\"ordo\")]");
    assertThat(allMetadata.size(), is(1));
    LinkedHashMap<String, String> ordoMetadata = allMetadata.get(0);
    assertThat(ordoMetadata.get("namespacePrefix"), equalTo("ORDO"));
    assertThat(ordoMetadata.get("name"), equalTo("Orphanet Rare Disease Ontology"));
    assertThat(
        ordoMetadata.get("url"), equalTo("http://www.orphadata.org/data/ORDO/ordo_orphanet.owl"));
  }

  @Override
  protected void phaseThree() {}

  @Override
  protected void phaseFour() {}

  @Override
  protected void phaseFive() {}

  private Sample getTestSample() {
    Sample.Builder sampleBuilder =
        new Sample.Builder("Phenopacket_ERS1790018", "Phenopacket_ERS1790018");
    sampleBuilder
        .withDomain(this.defaultIntegrationSubmissionDomain)
        .withRelease("2017-01-01T12:00:00")
        .withUpdate("2017-01-01T12:00:00")
        .withTaxId(Long.valueOf(9606))
        .withAttributes(
            Arrays.asList(
                Attribute.build(
                    "Organism",
                    "Homo sapiens",
                    "http://purl.obolibrary.org/obo/NCBITaxon_9606",
                    null),
                Attribute.build(
                    "cell type", "myoblast", "http://purl.obolibrary.org/obo/CL_0000056", null),
                Attribute.build(
                    "disease state",
                    "Duchenne muscular dystrophy",
                    "http://www.orpha.net/ORDO/Orphanet_98896",
                    null),
                Attribute.build("genotype", "BMI1 overexpression"),
                Attribute.build("individual", "SD-8306I")));

    return sampleBuilder.build();
  }
}
