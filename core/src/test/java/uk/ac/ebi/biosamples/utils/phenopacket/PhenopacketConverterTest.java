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
package uk.ac.ebi.biosamples.utils.phenopacket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.phenopackets.schema.v1.Phenopacket;
import org.springframework.core.io.ClassPathResource;
import uk.ac.ebi.biosamples.core.model.Attribute;
import uk.ac.ebi.biosamples.core.model.Sample;

public class PhenopacketConverterTest {
  private final PhenopacketConverter phenopacketConverter =
      new PhenopacketConverter(new PhenopacketConversionHelper());
  private final ObjectMapper jsonMapper = new ObjectMapper();

  @Test
  @Ignore
  public void testPhenopacketConversion() throws Exception {
    final Sample sample = getTestSample();
    final Phenopacket phenopacket = phenopacketConverter.convert(sample);

    final JsonNode phenopacketExpected =
        jsonMapper.readValue(
            new ClassPathResource("phenopacket/phenopacket_1.json").getInputStream(),
            JsonNode.class);
    Assert.assertEquals(
        phenopacketExpected.get("biosamples").get(0).get("id").textValue(),
        phenopacket.getBiosamples(0).getId());
  }

  @Test
  public void testPhenopacketConversion_withoutOrganismOntology() {
    final Sample sample = getTestSampleWithoutOrganismOntology();
    final Phenopacket phenopacket = phenopacketConverter.convert(sample);
    Assert.assertSame("", phenopacket.getBiosamples(0).getTaxonomy().getId());
  }

  @Test
  @Ignore
  public void testPhenopacketConversion_resources() throws Exception {
    final Sample sample = getTestSample_2();
    final Phenopacket phenopacket = phenopacketConverter.convert(sample);
    Assert.assertTrue(phenopacket.getMetaData().getResourcesList().size() > 0);
  }

  private Sample getTestSample() {
    return new Sample.Builder("phenopacket_test")
        .withAccession("SAMETAG2031")
        .withDomain("self.BiosampleIntegrationTest")
        .withRelease("2017-01-01T12:00:00")
        .withUpdate("2017-01-01T12:00:00")
        .addAttribute(
            Attribute.build(
                "organism", "human", "http://purl.obolibrary.org/obo/NCBITaxon_9606", ""))
        .addAttribute(
            Attribute.build(
                "disease", "colorectal adenocarcinoma", "http://www.ebi.ac.uk/efo/EFO_0000365", ""))
        .addAttribute(
            Attribute.build("sex", "male", "http://purl.obolibrary.org/obo/PATO_0000384", ""))
        .addAttribute(Attribute.build("age", "30"))
        .addAttribute(Attribute.build("test", "test", "http://purl.obolibrary.org/obo/test", ""))
        .addAttribute(Attribute.build("description", "test description"))
        .addAttribute(
            Attribute.build("tissue", "liver", "http://purl.obolibrary.org/obo/UBERON_0002107", ""))
        .addAttribute(
            Attribute.build(
                "lung disease", "yes", "http://purl.obolibrary.org/obo/UBERON_0002107", ""))
        .addAttribute(
            Attribute.build(
                "disease state",
                "Duchenne muscular dystrophy",
                "http://www.orpha.net/ORDO/Orphanet_98896",
                null))
        .build();
  }

  private Sample getTestSampleWithoutOrganismOntology() {
    return new Sample.Builder("phenopacket_test")
        .withAccession("SAMETAG2031")
        .withDomain("self.BiosampleIntegrationTest")
        .withRelease("2017-01-01T12:00:00")
        .withUpdate("2017-01-01T12:00:00")
        .addAttribute(Attribute.build("organism", "human"))
        .build();
  }

  private Sample getTestSample_2() {
    final Sample.Builder sampleBuilder =
        new Sample.Builder("Phenopacket_ERS1790018", "Phenopacket_ERS1790018");
    sampleBuilder
        .withDomain("self.BiosampleIntegrationTest")
        .withRelease("2017-01-01T12:00:00")
        .withUpdate("2017-01-01T12:00:00")
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
