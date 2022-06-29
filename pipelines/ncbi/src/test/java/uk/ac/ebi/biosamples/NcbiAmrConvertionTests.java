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

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.dom4j.Element;
import org.junit.Before;
import org.junit.Test;
import uk.ac.ebi.biosamples.model.structured.StructuredDataEntry;
import uk.ac.ebi.biosamples.model.structured.StructuredDataTable;
import uk.ac.ebi.biosamples.model.structured.StructuredDataType;
import uk.ac.ebi.biosamples.ncbi.NcbiEraProDao;
import uk.ac.ebi.biosamples.ncbi.service.NcbiAmrConversionService;
import uk.ac.ebi.biosamples.ncbi.service.NcbiSampleConversionService;
import uk.ac.ebi.biosamples.service.TaxonomyService;
import uk.ac.ebi.biosamples.utils.XmlPathBuilder;

public class NcbiAmrConvertionTests {

  private NcbiSampleConversionService sampleConversionService;
  private NcbiAmrConversionService amrConversionService;

  @Before
  public void setUp() {
    sampleConversionService =
        new NcbiSampleConversionService(new TaxonomyService(), new NcbiEraProDao());
    amrConversionService = new NcbiAmrConversionService();
  }

  @Test
  public void given_amr_data_it_produces_sample_with_structured_data() {
    Set<StructuredDataTable> structuredData =
        sampleConversionService.convertNcbiXmlElementToStructuredData(
            getAmrSample(), new HashSet<>());
    assertEquals("Should contain only 1 AMR element", 1, structuredData.size());
    assertEquals(
        "Sample should contain AMR data",
        structuredData.iterator().next().getType(),
        StructuredDataType.AMR.name());
    assertEquals(
        "Should contain only 1 row", 1, structuredData.iterator().next().getContent().size());
  }

  @Test
  public void given_amr_xml_it_extracts_proper_content() {
    Set<StructuredDataTable> structuredData =
        sampleConversionService.convertNcbiXmlElementToStructuredData(
            getAmrSample(), new HashSet<>());
    StructuredDataTable structuredDataTable = structuredData.iterator().next();
    Map<String, StructuredDataEntry> dataRow = structuredDataTable.getContent().iterator().next();

    assertEquals(dataRow.get("antibioticName").getValue(), "nalidixic acid");
    assertEquals(dataRow.get("resistancePhenotype").getValue(), "intermediate");
    assertEquals(dataRow.get("measurementSign").getValue(), "==");
    assertEquals(dataRow.get("measurement").getValue(), "17");
    assertEquals(dataRow.get("measurementUnits").getValue(), "mm");
    assertEquals(dataRow.get("laboratoryTypingMethod").getValue(), "disk diffusion");
    assertEquals(dataRow.get("platform").getValue(), "missing");
    assertEquals(dataRow.get("laboratoryTypingMethodVersionOrReagent").getValue(), "missing");
    assertEquals(dataRow.get("vendor").getValue(), "Becton Dickinson");
    assertEquals(dataRow.get("astStandard").getValue(), "CLSI");
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

    Set<Map<String, StructuredDataEntry>> structuredTableSet;
    try {
      structuredTableSet = amrConversionService.convertStructuredTable(amrTableElement, organism);
    } catch (NcbiAmrConversionService.AmrParsingException e) {
      e.printStackTrace();
      throw e;
    }

    assertEquals(4, structuredTableSet.size());
  }

  @Test
  public void it_can_read_multiple_types_of_antibiograms_table() {
    for (Element element : getAmrSampleSet()) {
      Set<StructuredDataTable> structuredData =
          sampleConversionService.convertNcbiXmlElementToStructuredData(
              getAmrSample(), new HashSet<>());
      assertNotNull(structuredData);
      assertFalse(structuredData.isEmpty());
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
