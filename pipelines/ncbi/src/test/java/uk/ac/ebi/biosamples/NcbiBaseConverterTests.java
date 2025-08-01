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

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;
import uk.ac.ebi.biosamples.core.model.Attribute;
import uk.ac.ebi.biosamples.core.model.Sample;
import uk.ac.ebi.biosamples.core.model.structured.StructuredDataTable;
import uk.ac.ebi.biosamples.ncbi.service.NcbiSampleConversionService;
import uk.ac.ebi.biosamples.service.TaxonomyService;

@RunWith(SpringRunner.class)
public class NcbiBaseConverterTests {

  private NcbiSampleConversionService conversionService;

  private Element testNcbiBioSamples;

  @Before
  public void setup() {
    conversionService = new NcbiSampleConversionService(new TaxonomyService());
    testNcbiBioSamples =
        NcbiTestsService.readNcbiBiosampleElementFromFile("/examples/ncbi_sample_5246317.xml");
  }

  @Test
  public void given_ncbi_biosample_extract_accession_name_synonym() {
    final Sample sampleToTest = conversionService.convertNcbiXmlElementToSample(testNcbiBioSamples);
    assertEquals(sampleToTest.getAccession(), "SAMN05246317");
    assertEquals(sampleToTest.getName(), "GF.26.AL.R");
  }

  @Test
  public void given_ncbi_biosample_extract_pubmeds() {
    final Sample sampleToTest = conversionService.convertNcbiXmlElementToSample(testNcbiBioSamples);
    assertEquals(1, sampleToTest.getPublications().size());
    assertTrue(
        sampleToTest.getPublications().stream()
            .filter(publication -> publication.getPubMedId().equals("20497546"))
            .findFirst()
            .isPresent());
  }

  @Test
  public void it_extracts_external_Ids() {
    final Sample sampleToTest = conversionService.convertNcbiXmlElementToSample(testNcbiBioSamples);

    final List<Attribute> expectedAttribute =
        sampleToTest.getAttributes().stream()
            .filter(attr -> attr.getType().equals("External Id"))
            .collect(Collectors.toList());
    assertTrue(expectedAttribute.size() == 4);
  }

  @Test
  public void given_ncbi_biosamples_it_generates_and_insdc_secondary_accession_attribute() {
    final Sample sampleToTest = conversionService.convertNcbiXmlElementToSample(testNcbiBioSamples);
    final Optional<Attribute> expectedAttribute =
        sampleToTest.getAttributes().stream()
            .filter(attr -> attr.getType().equals("INSDC secondary accession"))
            .findFirst();

    final Attribute secondaryAccession = expectedAttribute.get();
    assertEquals(secondaryAccession.getValue(), "SRS1524325");
  }

  @Test
  public void given_ncbi_biosamples_it_generates_and_insdc_first_public_attribute() {
    final Sample sampleToTest = conversionService.convertNcbiXmlElementToSample(testNcbiBioSamples);
    final Optional<Attribute> expectedAttribute =
        sampleToTest.getAttributes().stream()
            .filter(attr -> attr.getType().equals("INSDC first public"))
            .findFirst();

    final Attribute secondaryAccession = expectedAttribute.get();
    assertEquals(secondaryAccession.getValue(), "2018-07-01T00:50:05.513Z");
  }

  @Test
  public void given_ncbi_biosamples_it_generates_and_sra_accession_attribute() {
    final Sample sampleToTest = conversionService.convertNcbiXmlElementToSample(testNcbiBioSamples);
    final Optional<Attribute> expectedAttribute =
        sampleToTest.getAttributes().stream()
            .filter(attr -> attr.getType().equals("SRA accession"))
            .findFirst();

    final Attribute secondaryAccession = expectedAttribute.get();
    assertEquals(secondaryAccession.getValue(), "SRS1524325");
  }

  @Test
  public void it_extracts_insdc_center_name() {
    final Sample sampleToTest = conversionService.convertNcbiXmlElementToSample(testNcbiBioSamples);
    final Optional<Attribute> expectedAttribute =
        sampleToTest.getAttributes().stream()
            .filter(attr -> attr.getType().equals("INSDC center name"))
            .findFirst();
    assertTrue(expectedAttribute.isPresent());

    final Attribute centerName = expectedAttribute.get();
    assertEquals(centerName.getValue(), "Lund University");
  }

  @Test
  public void it_extracts_description_text_and_tag() {
    final Sample sampleToTest = conversionService.convertNcbiXmlElementToSample(testNcbiBioSamples);
    final Optional<Attribute> expectedAttributeType =
        sampleToTest.getAttributes().stream()
            .filter(attr -> attr.getType().equals("description"))
            .findFirst();
    assertTrue(expectedAttributeType.isPresent());

    final Attribute description = expectedAttributeType.get();
    assertEquals(description.getValue(), "Human HapMap individual Coriell catalog ID NA18582");
  }

  @Test
  public void it_extracts_common_name() {
    final Sample sampleToTest = conversionService.convertNcbiXmlElementToSample(testNcbiBioSamples);
    final Optional<Attribute> expectedAttribute =
        sampleToTest.getAttributes().stream()
            .filter(attr -> attr.getType().equals("common name"))
            .findFirst();
    assertTrue(expectedAttribute.isPresent());

    final Attribute commonName = expectedAttribute.get();
    assertEquals(commonName.getValue(), "gb|AMGQ00000000.1");
  }

  @Test
  public void it_extracts_create() {
    final Sample sampleToTest = conversionService.convertNcbiXmlElementToSample(testNcbiBioSamples);

    assertTrue(sampleToTest.getCreate() != null);
    assertEquals(sampleToTest.getCreate().toString(), "2010-06-14T13:47:08.137Z");
  }

  @Test
  public void it_extracts_submitted() {
    final Sample sampleToTest = conversionService.convertNcbiXmlElementToSample(testNcbiBioSamples);

    assertTrue(sampleToTest.getSubmitted() != null);
    assertEquals(sampleToTest.getSubmitted().toString(), "2010-06-14T13:47:08.137Z");
  }

  @Test
  public void it_extracts_organism_attribute() {
    final Sample sampleToTest = conversionService.convertNcbiXmlElementToSample(testNcbiBioSamples);
    final Optional<Attribute> expectedAttribute =
        sampleToTest.getAttributes().stream()
            .filter(attr -> attr.getType().equals("organism"))
            .findFirst();

    assertTrue(expectedAttribute.isPresent());

    final Attribute organism = expectedAttribute.get();
    assertEquals(organism.getValue(), "soil metagenome");
    assertEquals(organism.getIri().first(), "http://purl.obolibrary.org/obo/NCBITaxon_410658");
  }

  @Test
  public void it_extracts_description_title() {
    final Sample sampleToTest = conversionService.convertNcbiXmlElementToSample(testNcbiBioSamples);
    final Optional<Attribute> expectedAttribute =
        sampleToTest.getAttributes().stream()
            .filter(attr -> attr.getType().equals("title"))
            .findFirst();

    assertTrue(expectedAttribute.isPresent());

    final Attribute description = expectedAttribute.get();
    assertEquals(description.getValue(), "Metagenome or environmental sample from soil metagenome");
  }

  @Test
  public void it_extracts_attributes() {
    final Sample sampleToTest = conversionService.convertNcbiXmlElementToSample(testNcbiBioSamples);
    final SortedSet<Attribute> sampleAttributes = sampleToTest.getAttributes();

    final List<Attribute> attrWithTag =
        sampleAttributes.stream()
            .filter(
                attribute -> attribute.getTag() != null && attribute.getTag().equals("attribute"))
            .collect(Collectors.toList());

    // 6 user provided attributes at this moment so its hardcoded in the test
    assertTrue(attrWithTag.size() == 6);

    final List<Attribute> expectedAttributes =
        Stream.of(
                Attribute.build(
                    "isolation_source",
                    "Alseis blackiana roots",
                    "attribute",
                    Collections.emptyList(),
                    null),
                Attribute.build(
                    "collection_date", "Sep-2012", "attribute", Collections.emptyList(), null),
                Attribute.build(
                    "geo_loc_name",
                    "Panama:Gigante_peninsula",
                    "attribute",
                    Collections.emptyList(),
                    null),
                Attribute.build(
                    "lat_lon", "9.110057 N 79.8434 W", "attribute", Collections.emptyList(), null),
                Attribute.build(
                    "Fert_treat", "unfertilized", "attribute", Collections.emptyList(), null),
                Attribute.build("plot", "GF_26", "attribute", Collections.emptyList(), null))
            .collect(Collectors.toList());
    final Optional<Attribute> attributesNotMatching =
        expectedAttributes.stream().filter(attr -> !sampleAttributes.contains(attr)).findAny();

    assertTrue(!attributesNotMatching.isPresent());
  }

  @Test
  public void it_extracts_ncbi_submission_model() {
    final Sample sampleToTest = conversionService.convertNcbiXmlElementToSample(testNcbiBioSamples);
    final Optional<Attribute> expectedAttribute =
        sampleToTest.getAttributes().stream()
            .filter(attr -> attr.getType().equals("NCBI submission model"))
            .findFirst();

    assertTrue(expectedAttribute.isPresent());

    final Attribute ncbiSubmissionModel = expectedAttribute.get();
    assertEquals(ncbiSubmissionModel.getValue(), "Metagenome or environmental");
  }

  @Test
  public void it_extracts_ncbi_submission_package() {
    final Sample sampleToTest = conversionService.convertNcbiXmlElementToSample(testNcbiBioSamples);
    final Optional<Attribute> expectedAttribute =
        sampleToTest.getAttributes().stream()
            .filter(attr -> attr.getType().equals("NCBI submission package"))
            .findFirst();

    assertTrue(expectedAttribute.isPresent());

    final Attribute ncbiSubmissionPackage = expectedAttribute.get();
    assertEquals(ncbiSubmissionPackage.getValue(), "Metagenome.environmental.2.0");
  }

  @Test
  public void it_extracts_insdc_dates() {
    final Sample sampleToTest = conversionService.convertNcbiXmlElementToSample(testNcbiBioSamples);
    Optional<Attribute> expectedAttribute =
        sampleToTest.getAttributes().stream()
            .filter(attr -> attr.getType().equals("INSDC first public"))
            .findFirst();

    assertTrue(expectedAttribute.isPresent());

    final Attribute insdcFirstPublic = expectedAttribute.get();
    assertEquals(insdcFirstPublic.getValue(), "2018-07-01T00:50:05.513Z");

    expectedAttribute =
        sampleToTest.getAttributes().stream()
            .filter(attr -> attr.getType().equals("INSDC last update"))
            .findFirst();

    assertTrue(expectedAttribute.isPresent());

    final Attribute insdcLastUpdate = expectedAttribute.get();
    assertEquals(insdcLastUpdate.getValue(), "2018-07-01T00:50:05.513Z");
  }

  @Test
  public void it_extracts_insdc_status() {
    final Sample sampleToTest = conversionService.convertNcbiXmlElementToSample(testNcbiBioSamples);
    final Optional<Attribute> expectedAttribute =
        sampleToTest.getAttributes().stream()
            .filter(attr -> attr.getType().equals("INSDC status"))
            .findFirst();

    assertTrue(expectedAttribute.isPresent());

    final Attribute insdcStatus = expectedAttribute.get();
    assertEquals(insdcStatus.getValue(), "live");
  }

  @Test
  public void given_ncbi_status_not_live_it_set_release_date_in_the_future()
      throws DocumentException {
    final Element ncbiSampleNotLive =
        readBioSampleElementFromXml("/examples/ncbi_test_sample_not_live.xml");
    final Sample sampleToTest = conversionService.convertNcbiXmlElementToSample(ncbiSampleNotLive);
    final Optional<Attribute> expectedAttribute =
        sampleToTest.getAttributes().stream()
            .filter(attr -> attr.getType().equals("INSDC status"))
            .findFirst();

    assertTrue(expectedAttribute.isPresent());
    assertNotEquals(expectedAttribute.get().getValue(), "live");

    // Sample release date is set in the future
    assertTrue(sampleToTest.getRelease().isAfter(Instant.parse("3018-07-01T00:50:05.00Z")));
  }

  @Test
  public void
      given_ncbi_sample_with_multiple_amr_tables_there_can_be_only_one_table_with_same_type_and_submitter()
          throws DocumentException {
    final Element ncbiSampleWithMultipleAMR =
        readBioSampleElementFromXml("/examples/ncbi_amr_sample_with_multiple_amr_entries.xml");
    final Set<StructuredDataTable> amrDataSet =
        conversionService.convertNcbiXmlElementToStructuredData(
            ncbiSampleWithMultipleAMR, Collections.emptySet());

    assertNotNull(amrDataSet);
    assertEquals(1, amrDataSet.size());
  }

  public Element readBioSampleElementFromXml(final String pathToFile) throws DocumentException {
    final InputStream xmlInputStream = getClass().getResourceAsStream(pathToFile);
    final String xmlDocument =
        new BufferedReader(new InputStreamReader(xmlInputStream))
            .lines()
            .collect(Collectors.joining());
    final Document doc = DocumentHelper.parseText(xmlDocument);
    return doc.getRootElement().element("BioSample");
  }
}
