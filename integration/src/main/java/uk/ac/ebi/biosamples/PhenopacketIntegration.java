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

import com.jayway.jsonpath.JsonPath;
import java.net.URI;
import java.util.*;
import org.hamcrest.CoreMatchers;
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
  private final RestTemplate restTemplate;

  public PhenopacketIntegration(
      final BioSamplesClient client, final RestTemplateBuilder restTemplateBuilder) {
    super(client);

    restTemplate = restTemplateBuilder.build();
  }

  @Override
  protected void phaseOne() {
    final Sample testSample = getTestSample();
    final Optional<EntityModel<Sample>> optionalSample =
        client.fetchSampleResource(testSample.getAccession());

    if (optionalSample.isPresent()) {
      throw new RuntimeException("Phenopacket test sample should not be available during phase 1");
    }

    final EntityModel<Sample> resource = client.persistSampleResource(testSample);
    final Attribute sraAccessionAttribute =
        Objects.requireNonNull(resource.getContent()).getAttributes().stream()
            .filter(attribute -> attribute.getType().equals(BioSamplesConstants.SRA_ACCESSION))
            .findFirst()
            .get();

    testSample.getAttributes().add(sraAccessionAttribute);

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
    /*this.checkSampleWithOrphanetLinkWorks();*/
  }

  private void checkSampleWithOrphanetLinkWorks() {
    final Sample testSample = getTestSample();
    final Optional<EntityModel<Sample>> sampleResource =
        client.fetchSampleResource(testSample.getAccession());

    assertThat(sampleResource.isPresent(), CoreMatchers.is(true));

    final URI sampleURI = URI.create(sampleResource.get().getLink("self").get().getHref());
    final MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();

    headers.add("Accept", "application/phenopacket+json");

    final RequestEntity request = new RequestEntity<>(headers, HttpMethod.GET, sampleURI);
    final ResponseEntity<String> response = restTemplate.exchange(request, String.class);

    if (!response.getStatusCode().is2xxSuccessful()) {
      throw new RuntimeException(
          "Impossible to retrieve correctly phenopacket sample with name " + testSample.getName());
    }

    final List<LinkedHashMap> allMetadata =
        JsonPath.read(response.getBody(), "$.metaData.resources[?(@.id==\"ordo\")]");

    assertThat(allMetadata.size(), is(1));

    final LinkedHashMap<String, String> ordoMetadata = allMetadata.get(0);

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

  @Override
  protected void phaseSix() {}

  private Sample getTestSample() {
    final Sample.Builder sampleBuilder =
        new Sample.Builder("Phenopacket_ERS1790018", "Phenopacket_ERS1790018");

    sampleBuilder
        .withDomain(defaultIntegrationSubmissionDomain)
        .withRelease("2017-01-01T12:00:00")
        .withUpdate("2017-01-01T12:00:00")
        .withTaxId(9606L)
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
