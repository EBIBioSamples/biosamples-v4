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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.Instant;
import java.util.*;
import org.springframework.hateoas.EntityModel;
import org.springframework.stereotype.Component;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.Publication;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.structured.StructuredData;
import uk.ac.ebi.biosamples.utils.IntegrationTestFailException;
import uk.ac.ebi.biosamples.utils.TestUtilities;

@Component
public class StructuredDataGenericIntegration extends AbstractIntegration {
  private final ObjectMapper mapper;

  public StructuredDataGenericIntegration(final BioSamplesClient client) {
    super(client);
    mapper = new ObjectMapper();
  }

  @Override
  protected void phaseOne() {
    final Sample testSample = getSampleTest1();
    final Optional<Sample> optionalSample = fetchUniqueSampleByName(testSample.getName());

    final String accession;
    if (optionalSample.isPresent()) {
      throw new IntegrationTestFailException(
          "RestIntegration test sample should not be available during phase 1", Phase.ONE);
    } else {
      final EntityModel<Sample> resource = client.persistSampleResource(testSample);
      final Sample sampleContent = resource.getContent();

      Sample testSampleWithAccession =
          Sample.Builder.fromSample(testSample)
              .withAccession(Objects.requireNonNull(sampleContent).getAccession())
              .withStatus(sampleContent.getStatus())
              .build();

      final Attribute sraAccessionAttribute4 =
          resource.getContent().getAttributes().stream()
              .filter(attribute -> attribute.getType().equals("SRA accession"))
              .findFirst()
              .get();

      testSampleWithAccession.getAttributes().add(sraAccessionAttribute4);
      testSampleWithAccession = Sample.Builder.fromSample(testSampleWithAccession).build();

      accession = sampleContent.getAccession();

      if (!testSampleWithAccession.equals(sampleContent)) {
        throw new IntegrationTestFailException(
            "Expected response (" + sampleContent + ") to equal submission (" + testSample + ")");
      }
    }

    final String json = TestUtilities.readFileAsString("structured_data.json");
    StructuredData structuredData;
    try {
      structuredData = mapper.readValue(json, StructuredData.class);
      structuredData = StructuredData.build(accession, Instant.now(), structuredData.getData());
    } catch (final IOException e) {
      throw new IntegrationTestFailException(
          "An error occurred while converting json to Sample class" + e, Phase.ONE);
    }

    final EntityModel<StructuredData> dataResource = client.persistStructuredData(structuredData);
    if (!structuredData.equals(dataResource.getContent())) {
      try {
        final String expected = mapper.writeValueAsString(structuredData);
        final String actual = mapper.writeValueAsString(dataResource.getContent());
        throw new IntegrationTestFailException(
            "Expected: " + expected + ", found: " + actual, Phase.ONE);
      } catch (final JsonProcessingException e) {
        throw new IntegrationTestFailException(
            "Expected: " + structuredData + ", found: " + dataResource.getContent(), Phase.ONE);
      }
    }
  }

  @Override
  protected void phaseTwo() {
    final Sample sampleTest1 = getSampleTest1();
    final Optional<Sample> optionalSample = fetchUniqueSampleByName(sampleTest1.getName());
    if (!optionalSample.isPresent()) {
      throw new IntegrationTestFailException(
          "Cant find sample " + sampleTest1.getName(), Phase.TWO);
    }

    final Sample sample =
        optionalSample.orElseThrow(
            () ->
                new IntegrationTestFailException(
                    "Cant find sample " + sampleTest1.getName(), Phase.TWO));
    if (sample.getStructuredData().isEmpty()) {
      throw new IntegrationTestFailException(
          "No structured data in sample " + sampleTest1.getName(), Phase.TWO);
    }
  }

  @Override
  protected void phaseThree() {
    // skip
  }

  @Override
  protected void phaseFour() {
    // skip
  }

  @Override
  protected void phaseFive() {
    // skip
  }

  @Override
  protected void phaseSix() {}

  private Sample getSampleTest1() {
    final String name = "StructuredDataGenericIntegration_sample_1";
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
        .withDomain(defaultIntegrationSubmissionDomain)
        .withRelease(release)
        .withUpdate(update)
        .withPublications(Collections.singletonList(publication))
        .withAttributes(attributes)
        .build();
  }
}
