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

import static org.junit.Assert.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.hateoas.EntityModel;
import org.springframework.stereotype.Component;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.client.utils.ClientProperties;
import uk.ac.ebi.biosamples.core.model.*;
import uk.ac.ebi.biosamples.core.model.structured.StructuredData;
import uk.ac.ebi.biosamples.core.model.structured.StructuredDataEntry;
import uk.ac.ebi.biosamples.core.model.structured.StructuredDataTable;
import uk.ac.ebi.biosamples.core.model.structured.StructuredDataType;
import uk.ac.ebi.biosamples.utils.IntegrationTestFailException;
import uk.ac.ebi.biosamples.utils.TestUtilities;

@Component
public class AmrDataIntegration extends AbstractIntegration {
  private final Logger log = LoggerFactory.getLogger(getClass());
  private final ObjectMapper mapper;
  private final ClientProperties clientProperties;

  public AmrDataIntegration(
      final BioSamplesClient client,
      final RestTemplateBuilder restTemplateBuilder,
      ClientProperties clientProperties) {
    super(client);

    this.clientProperties = clientProperties;
    restTemplateBuilder.build();

    mapper = new ObjectMapper();
  }

  @Override
  protected void phaseOne() {
    final Sample testSample = getTestSample();
    final Optional<Sample> optionalSample = fetchUniqueSampleByName(testSample.getName());

    if (optionalSample.isPresent()) {
      throw new IntegrationTestFailException(
          "AMRDataIntegration test sample should not be available during phase 1", Phase.ONE);
    }

    final EntityModel<Sample> resource = webinClient.persistSampleResource(testSample);

    Sample testSampleWithAccession =
        Sample.Builder.fromSample(testSample)
            .withAccession(Objects.requireNonNull(resource.getContent()).getAccession())
            .build();

    testSampleWithAccession =
        Sample.Builder.fromSample(testSampleWithAccession)
            .withStatus(resource.getContent().getStatus())
            .build();

    final Attribute sraAccessionAttribute =
        resource.getContent().getAttributes().stream()
            .filter(attribute -> attribute.getType().equals("SRA accession"))
            .findFirst()
            .get();

    testSampleWithAccession.getAttributes().add(sraAccessionAttribute);

    if (!testSampleWithAccession.equals(resource.getContent())) {
      throw new IntegrationTestFailException(
          "Expected response ("
              + resource.getContent()
              + ") to equal submission ("
              + testSample
              + ")");
    }
  }

  @Override
  protected void phaseTwo() {
    final Sample testSample = getTestSample();
    final Optional<Sample> optionalSample = fetchUniqueSampleByName(testSample.getName());

    if (!optionalSample.isPresent()) {
      throw new IntegrationTestFailException("Cant find sample " + testSample.getName(), Phase.TWO);
    }

    final String json = TestUtilities.readFileAsString("structured_data_amr.json");

    StructuredData sd;

    try {
      sd = mapper.readValue(json, StructuredData.class);
    } catch (final IOException e) {
      throw new RuntimeException(
          "An error occurred while converting json to structured data class", e);
    }

    sd = StructuredData.build(optionalSample.get().getAccession(), sd.getCreate(), sd.getData());

    final EntityModel<StructuredData> structuredDataResource =
        webinClient.persistStructuredData(sd);

    if (structuredDataResource.getContent() == null) {
      throw new RuntimeException("Should return submitted structured data");
    }
  }

  @Override
  protected void phaseThree() throws InterruptedException {
    final Sample testSample = getTestSample();

    TimeUnit.SECONDS.sleep(2);

    final Optional<Sample> optionalSample = fetchUniqueSampleByName(testSample.getName());

    if (optionalSample.isEmpty()) {
      throw new IntegrationTestFailException(
          "Cant find sample " + testSample.getName(), Phase.THREE);
    }

    final Sample amrSample = optionalSample.get();

    log.info("Checking sample has amr data");

    assertEquals(amrSample.getStructuredData().size(), 1);
    assertEquals(
        amrSample.getStructuredData().iterator().next().getType(), StructuredDataType.AMR.name());

    final StructuredDataTable table = amrSample.getStructuredData().iterator().next();

    assertEquals(table.getContent().size(), 15);

    // Assert there are only 2 entries with missing testing standard
    assertEquals(
        table
            .getContent()
            .parallelStream()
            .filter(entry -> entry.get("ast_standard").getValue().equalsIgnoreCase("missing"))
            .count(),
        2);

    // Verifying AMREntry for ciprofloxacin is found and has certain values
    final Optional<Map<String, StructuredDataEntry>> optionalAmrEntry =
        table
            .getContent()
            .parallelStream()
            .filter(
                entry -> entry.get("antibiotic_name").getValue().equalsIgnoreCase("ciprofloxacin"))
            .findFirst();
    if (!optionalAmrEntry.isPresent()) {
      throw new RuntimeException(
          "AMRentry for antibiotic ciprofloxacin should be present but is not");
    }

    assertEquals(optionalAmrEntry.get().get("resistance_phenotype").getValue(), "susceptible");
    assertEquals(optionalAmrEntry.get().get("measurement_sign").getValue(), "<=");
    assertEquals(optionalAmrEntry.get().get("vendor").getValue(), "Trek");
    assertEquals(optionalAmrEntry.get().get("platform").getValue(), "");

    //    assertEquals(ciprofloxacin.getResistancePhenotype(), "susceptible");
    //    assertEquals(ciprofloxacin.getMeasurementSign(), "<=");
    //    assertEquals(ciprofloxacin.getMeasurement(), "0.015");
    //    assertEquals(ciprofloxacin.getMeasurementUnits(), "mg/L");
    //    assertEquals(ciprofloxacin.getLaboratoryTypingMethod(), "MIC");
    //    assertEquals(ciprofloxacin.getPlatform(), "");
    //    assertEquals(ciprofloxacin.getLaboratoryTypingMethodVersionOrReagent(), "96-Well Plate");
    //    assertEquals(ciprofloxacin.getVendor(), "Trek");
    //    assertEquals(ciprofloxacin.getAstStandard(), "CLSI");
  }

  @Override
  protected void phaseFour() {}

  @Override
  protected void phaseFive() {}

  @Override
  protected void phaseSix() {}

  private Sample getTestSample() {
    final String name = "AMR_Data_Integration_sample_1";
    final Instant update = Instant.parse("2016-05-05T11:36:57.00Z");
    final Instant release = Instant.parse("2016-04-01T11:36:57.00Z");
    final SortedSet<Attribute> attributes = new TreeSet<>();

    attributes.add(Attribute.build("organism", "Chicken", null, null));
    attributes.add(Attribute.build("age", "3", null, Collections.emptyList(), "year"));
    attributes.add(Attribute.build("organism part", "heart"));

    final SortedSet<Relationship> relationships = new TreeSet<>();
    final SortedSet<ExternalReference> externalReferences = new TreeSet<>();
    final SortedSet<Organization> organizations = new TreeSet<>();
    final SortedSet<Contact> contacts = new TreeSet<>();
    final SortedSet<Publication> publications = new TreeSet<>();

    return new Sample.Builder(name)
        .withUpdate(update)
        .withRelease(release)
        .withWebinSubmissionAccountId(clientProperties.getBiosamplesClientWebinUsername())
        .withAttributes(attributes)
        .withRelationships(relationships)
        .withExternalReferences(externalReferences)
        .withOrganizations(organizations)
        .withContacts(contacts)
        .withPublications(publications)
        .build();
  }
}
