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
package uk.ac.ebi.biosamples.ncbi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Optional;
import java.util.stream.Collectors;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;
import uk.ac.ebi.biosamples.NcbiTestsService;
import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.ncbi.service.NcbiSampleConversionService;
import uk.ac.ebi.biosamples.service.TaxonomyService;

@RunWith(SpringRunner.class)
public class NcbiDatesTests {

  private NcbiSampleConversionService conversionService;

  private Element testNcbiBioSamples;

  @Before
  public void setup() {
    conversionService = new NcbiSampleConversionService(new TaxonomyService());
    testNcbiBioSamples =
        NcbiTestsService.readNcbiBiosampleElementFromFile("/examples/biosample_result_test.xml");
  }

  @Test
  public void given_ncbi_biosamples_it_generates_and_insdc_first_public_attribute() {
    final Sample sampleToTest = conversionService.convertNcbiXmlElementToSample(testNcbiBioSamples);
    final Optional<Attribute> expectedAttribute =
        sampleToTest.getAttributes().stream()
            .filter(attr -> attr.getType().equals("INSDC first public"))
            .findFirst();
    final Attribute secondaryAccession = expectedAttribute.get();

    assertEquals(secondaryAccession.getValue(), "2019-05-30T00:00:00Z");
  }

  @Test
  public void it_extracts_create() {
    final Sample sampleToTest = conversionService.convertNcbiXmlElementToSample(testNcbiBioSamples);

    assertTrue(sampleToTest.getCreate() != null);
    assertEquals(sampleToTest.getCreate().toString(), "2019-05-30T14:12:04.443Z");
    System.out.println(sampleToTest);
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

    assertEquals(insdcFirstPublic.getValue(), "2019-05-30T00:00:00Z");

    expectedAttribute =
        sampleToTest.getAttributes().stream()
            .filter(attr -> attr.getType().equals("INSDC last update"))
            .findFirst();

    assertTrue(expectedAttribute.isPresent());

    final Attribute insdcLastUpdate = expectedAttribute.get();

    assertEquals(insdcLastUpdate.getValue(), "2019-11-29T13:12:34.104Z");
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
