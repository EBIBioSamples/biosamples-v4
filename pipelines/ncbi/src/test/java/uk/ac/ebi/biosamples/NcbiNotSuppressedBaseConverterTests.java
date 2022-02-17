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
import static org.junit.Assert.assertTrue;

import java.util.Optional;
import org.dom4j.Element;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;
import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.ncbi.service.NcbiSampleConversionService;
import uk.ac.ebi.biosamples.utils.TaxonomyService;

@RunWith(SpringRunner.class)
public class NcbiNotSuppressedBaseConverterTests {

  private NcbiSampleConversionService conversionService;

  @Before
  public void setup() {
    this.conversionService = new NcbiSampleConversionService(new TaxonomyService());
  }

  @Test
  public void given_ncbi_live_biosample_ensure_live() {
    this.conversionService = new NcbiSampleConversionService(new TaxonomyService());
    Element testNcbiBioSamples =
        NcbiTestsService.readNcbiBiosampleElementFromFile("/examples/ncbi_sample_6685496.xml");
    Sample sampleToTest =
        this.conversionService.convertNcbiXmlElementToSample(testNcbiBioSamples);
    assertEquals(sampleToTest.getAccession(), "SAMN06685496");
    Optional<Attribute> expectedAttribute =
        sampleToTest.getAttributes().stream()
            .filter(attr -> attr.getType().equals("INSDC status"))
            .findFirst();
    assertTrue(expectedAttribute.isPresent());
    Attribute insdcStatus = expectedAttribute.get();
    assertEquals(insdcStatus.getValue(), "live");
  }

  @Test
  public void given_ncbi_suppressed_biosample_ensure_suppressed() {
    this.conversionService = new NcbiSampleConversionService(new TaxonomyService());
    Element testNcbiBioSamples =
        NcbiTestsService.readNcbiBiosampleElementFromFile("/examples/ncbi_sample_1553882.xml");
    Sample sampleToTest =
        this.conversionService.convertNcbiXmlElementToSample(testNcbiBioSamples);
    assertEquals(sampleToTest.getAccession(), "SAMN01553882");
    Optional<Attribute> expectedAttribute =
        sampleToTest.getAttributes().stream()
            .filter(attr -> attr.getType().equals("INSDC status"))
            .findFirst();
    assertTrue(expectedAttribute.isPresent());
    Attribute insdcStatus = expectedAttribute.get();
    assertEquals(insdcStatus.getValue(), "suppressed");
  }
}
