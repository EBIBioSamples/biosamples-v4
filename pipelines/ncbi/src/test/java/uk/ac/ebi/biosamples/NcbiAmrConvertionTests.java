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

import java.util.*;
import org.dom4j.Element;
import org.junit.Before;
import org.junit.Test;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.structured.AbstractData;
import uk.ac.ebi.biosamples.model.structured.StructuredDataType;
import uk.ac.ebi.biosamples.model.structured.amr.AMREntry;
import uk.ac.ebi.biosamples.model.structured.amr.AMRTable;
import uk.ac.ebi.biosamples.ncbi.service.NcbiAmrConversionService;
import uk.ac.ebi.biosamples.ncbi.service.NcbiSampleConversionService;
import uk.ac.ebi.biosamples.utils.TaxonomyService;
import uk.ac.ebi.biosamples.utils.XmlPathBuilder;

public class NcbiAmrConvertionTests {

  private NcbiSampleConversionService sampleConversionService;
  private NcbiAmrConversionService amrConversionService;

  @Before
  public void setUp() {
    sampleConversionService = new NcbiSampleConversionService(new TaxonomyService());
    amrConversionService = new NcbiAmrConversionService();
  }

  @Test
  public void given_amr_data_it_produces_sample_with_structured_data() {
    Sample sampleToTest =
        sampleConversionService.convertNcbiXmlElementToSample(getAmrSample(), new HashSet<>());
    Iterator<AbstractData> dataIterator = sampleToTest.getData().iterator();
    assertTrue(
        "Sample should contain AMR data",
        dataIterator.next().getDataType().equals(StructuredDataType.AMR));
    assertFalse("Should only contain AMR data", dataIterator.hasNext());
  }

  @Test
  public void it_extract_amr_rows() {
    Sample sampleToTest =
        sampleConversionService.convertNcbiXmlElementToSample(getAmrSample(), new HashSet<>());
    AbstractData data = sampleToTest.getData().iterator().next();
    assertTrue(data instanceof AMRTable);

    AMRTable amrTable = (AMRTable) data;
    assertTrue(
        "Should contain exactly 1 AmrEntry but found " + amrTable.getStructuredData().size(),
        amrTable.getStructuredData().size() == 1);
  }

  @Test
  public void it_extract_proper_content() {
    Sample sampleToTest =
        sampleConversionService.convertNcbiXmlElementToSample(getAmrSample(), new HashSet<>());
    AMRTable amrTable = (AMRTable) sampleToTest.getData().iterator().next();
    AMREntry amrEntry = amrTable.getStructuredData().iterator().next();

    assertEquals(amrEntry.getAntibioticName().getValue(), "nalidixic acid");
    assertEquals(amrEntry.getResistancePhenotype(), "intermediate");
    assertEquals(amrEntry.getMeasurementSign(), "==");
    assertEquals(amrEntry.getMeasurement(), "17");
    assertEquals(amrEntry.getMeasurementUnits(), "mm");
    assertEquals(amrEntry.getLaboratoryTypingMethod(), "disk diffusion");
    assertEquals(amrEntry.getPlatform(), "missing");
    assertEquals(amrEntry.getLaboratoryTypingMethodVersionOrReagent(), "missing");
    assertEquals(amrEntry.getVendor(), "Becton Dickinson");
    assertEquals(amrEntry.getAstStandard(), "CLSI");
  }

  @Test
  public void it_extract_multiple_entries_from_a_table()
      throws NcbiAmrConversionService.AmrParsingException {
    Element sampleWithMultipleAmrEntries = getBioSampleFromAmrSampleSet("SAMN09492289");
    Element amrTableElement =
        XmlPathBuilder.of(sampleWithMultipleAmrEntries)
            .path("Description", "Comment", "Table")
            .element();
    String organism =
        XmlPathBuilder.of(sampleWithMultipleAmrEntries)
            .path("Description", "Organism", "OrganismName")
            .text();

    AMRTable amrTable = null;

    try {
      amrTable = amrConversionService.convertElementToAmrTable(amrTableElement, organism);
    } catch (NcbiAmrConversionService.AmrParsingException e) {
      e.printStackTrace();
      throw e;
    }

    assertTrue(amrTable.getStructuredData().size() == 4);
  }

  @Test
  public void it_can_read_multiple_types_of_antibiograms_table() {
    for (Element element : getAmrSampleSet()) {
      Sample testSample =
          sampleConversionService.convertNcbiXmlElementToSample(element, new HashSet<>());
      AMRTable amrTable = (AMRTable) testSample.getData().first();
      assertTrue(amrTable != null);
      assertTrue(amrTable.getStructuredData().size() > 0);
    }
  }

  private Element getAmrSample() {
    return NcbiTestsService.readNcbiBiosampleElementFromFile("/examples/ncbi_amr_sample.xml");
  }

  public List<Element> getAmrSampleSet() {
    return NcbiTestsService.readNcbiBioSampleElementsFromFile("/examples/ncbi_amr_sample_set.xml");
  }

  public Element getBioSampleFromAmrSampleSet(String accession) {
    List<Element> biosamples = getAmrSampleSet();
    Optional<Element> element =
        biosamples.stream()
            .filter(elem -> elem.attributeValue("accession").equals(accession))
            .findFirst();
    if (!element.isPresent()) {
      throw new RuntimeException(
          "Unable to find BioSample with accession " + accession + " in the AMR sample set");
    }

    return element.get();
  }
}
