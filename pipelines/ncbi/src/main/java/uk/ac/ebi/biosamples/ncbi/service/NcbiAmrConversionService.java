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
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.dom4j.Element;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.model.structured.amr.AMREntry;
import uk.ac.ebi.biosamples.model.structured.amr.AMRTable;
import uk.ac.ebi.biosamples.model.structured.amr.AmrPair;
import uk.ac.ebi.biosamples.utils.XmlPathBuilder;

@Service
public class NcbiAmrConversionService {
  public AMRTable convertElementToAmrTable(Element amrTableElement, String organism)
      throws AmrParsingException {
    AMRTable.Builder amrTableBuilder =
        new AMRTable.Builder("test", "self.BiosampleImportNCBI", null);

    List<String> fields =
        XmlPathBuilder.of(amrTableElement).path("Header").elements("Cell").stream()
            .map(Element::getText)
            .collect(Collectors.toList());

    for (Element tableRow : XmlPathBuilder.of(amrTableElement).path("Body").elements("Row")) {

      AMREntry amrEntry = this.convertAmrEntry(tableRow, fields, organism);
      amrTableBuilder.addEntry(amrEntry);
    }

    return amrTableBuilder.build();
  }

  /**
   * Given a xml <Row> element correspondent to amr row, generate the AMR entry
   *
   * @param amrRowElement the Row element
   * @param fields the corresponding headers from the table
   * @param organism the organism associated with the AMR table
   * @return the AMR entry
   * @throws AmrParsingException if parse fails
   */
  private AMREntry convertAmrEntry(Element amrRowElement, List<String> fields, String organism)
      throws AmrParsingException {
    List<String> cells =
        XmlPathBuilder.of(amrRowElement).elements("Cell").stream()
            .map(Element::getText)
            .collect(Collectors.toList());

    if (cells.size() != fields.size()) {
      throw new AmrParsingException("Number of fields doesn't match number of values");
    }

    AMREntry.Builder amrEntryBuilder = new AMREntry.Builder();
    amrEntryBuilder.withSpecies(new AmrPair(organism));

    getFieldIfAvailable(cells, fields, "Antibiotic")
        .ifPresent(antibiotic -> amrEntryBuilder.withAntibioticName(new AmrPair(antibiotic, "")));
    getFieldIfAvailable(cells, fields, "Resistance phenotype")
        .ifPresent(amrEntryBuilder::withResistancePhenotype);
    getFieldIfAvailable(cells, fields, "Measurement sign")
        .ifPresent(amrEntryBuilder::withMeasurementSign);
    getFieldIfAvailable(cells, fields, "Measurement").ifPresent(amrEntryBuilder::withMeasurement);
    getFieldIfAvailable(cells, fields, "Measurement units")
        .ifPresent(amrEntryBuilder::withMeasurementUnits);
    getFieldIfAvailable(cells, fields, "Laboratory typing method")
        .ifPresent(amrEntryBuilder::withLaboratoryTypingMethod);
    getFieldIfAvailable(cells, fields, "Laboratory typing platform")
        .ifPresent(amrEntryBuilder::withPlatform);
    getFieldIfAvailable(cells, fields, "Laboratory typing method version or reagent")
        .ifPresent(amrEntryBuilder::withLaboratoryTypingMethodVersionOrReagent);
    getFieldIfAvailable(cells, fields, "Vendor").ifPresent(amrEntryBuilder::withVendor);
    getFieldIfAvailable(cells, fields, "Testing standard")
        .ifPresent(amrEntryBuilder::withAstStandard);

    return amrEntryBuilder.build();
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
