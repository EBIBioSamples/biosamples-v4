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
package uk.ac.ebi.biosamples.ncbi.service;

import java.text.ParseException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.dom4j.Element;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.model.structured.StructuredDataEntry;
import uk.ac.ebi.biosamples.utils.XmlPathBuilder;

@Service
public class NcbiAmrConversionService {
  public Set<Map<String, StructuredDataEntry>> convertStructuredTable(
      Element amrTableElement, String organism) throws AmrParsingException {
    List<String> fields =
        XmlPathBuilder.of(amrTableElement).path("Header").elements("Cell").stream()
            .map(Element::getText)
            .collect(Collectors.toList());

    Set<Map<String, StructuredDataEntry>> dataEntrySet = new HashSet<>();
    for (Element tableRow : XmlPathBuilder.of(amrTableElement).path("Body").elements("Row")) {
      dataEntrySet.add(getStructuredDataRow(tableRow, fields, organism));
    }

    return dataEntrySet;
  }

  /** Given a xml <Row> element correspondent to amr row, generate the AMR entry */
  private Map<String, StructuredDataEntry> getStructuredDataRow(
      Element amrRowElement, List<String> fields, String organism) throws AmrParsingException {
    List<String> cells =
        XmlPathBuilder.of(amrRowElement).elements("Cell").stream()
            .map(Element::getText)
            .collect(Collectors.toList());

    if (cells.size() != fields.size()) {
      throw new AmrParsingException("Number of fields doesn't match number of values");
    }

    Map<String, StructuredDataEntry> dataEntryMap = new HashMap<>();
    dataEntryMap.put("species", StructuredDataEntry.build(organism, null));
    getFieldIfAvailable(cells, fields, "Antibiotic")
        .ifPresent(d -> dataEntryMap.put("antibioticName", StructuredDataEntry.build(d, null)));
    getFieldIfAvailable(cells, fields, "Resistance phenotype")
        .ifPresent(
            d -> dataEntryMap.put("resistancePhenotype", StructuredDataEntry.build(d, null)));
    getFieldIfAvailable(cells, fields, "Measurement sign")
        .ifPresent(d -> dataEntryMap.put("measurementSign", StructuredDataEntry.build(d, null)));
    getFieldIfAvailable(cells, fields, "Measurement")
        .ifPresent(d -> dataEntryMap.put("measurement", StructuredDataEntry.build(d, null)));
    getFieldIfAvailable(cells, fields, "Measurement units")
        .ifPresent(d -> dataEntryMap.put("measurementUnits", StructuredDataEntry.build(d, null)));
    getFieldIfAvailable(cells, fields, "Laboratory typing method")
        .ifPresent(
            d -> dataEntryMap.put("laboratoryTypingMethod", StructuredDataEntry.build(d, null)));
    getFieldIfAvailable(cells, fields, "Laboratory typing platform")
        .ifPresent(d -> dataEntryMap.put("platform", StructuredDataEntry.build(d, null)));
    getFieldIfAvailable(cells, fields, "Laboratory typing method version or reagent")
        .ifPresent(
            d ->
                dataEntryMap.put(
                    "laboratoryTypingMethodVersionOrReagent", StructuredDataEntry.build(d, null)));
    getFieldIfAvailable(cells, fields, "Vendor")
        .ifPresent(d -> dataEntryMap.put("vendor", StructuredDataEntry.build(d, null)));
    getFieldIfAvailable(cells, fields, "Testing standard")
        .ifPresent(d -> dataEntryMap.put("astStandard", StructuredDataEntry.build(d, null)));

    return dataEntryMap;
  }

  /**
   * Extract a value from a list of values corresponding to the index of a string in a list of
   * string
   *
   * @param values the value list to extract from
   * @param fields the fields to use for checking
   * @param fieldToFind the field to extract
   * @return an optional string if the field is found
   */
  private Optional<String> getFieldIfAvailable(
      List<String> values, List<String> fields, String fieldToFind) {
    String fieldValue = null;

    if (fields.contains(fieldToFind)) {
      int fieldIndex = fields.indexOf(fieldToFind);
      fieldValue = values.get(fieldIndex);
    }

    return Optional.ofNullable(fieldValue);
  }

  public static class AmrParsingException extends ParseException {
    public AmrParsingException(String s) {
      super(s, -1);
    }
  }
}
