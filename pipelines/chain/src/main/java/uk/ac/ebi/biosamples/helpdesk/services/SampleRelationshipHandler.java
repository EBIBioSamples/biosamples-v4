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
package uk.ac.ebi.biosamples.helpdesk.services;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.hateoas.EntityModel;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.core.model.Relationship;
import uk.ac.ebi.biosamples.core.model.Sample;

@Service
@Slf4j
public class SampleRelationshipHandler {
  private final BioSamplesClient bioSamplesWebinClient;
  private final BioSamplesClient bioSamplesAapClient;

  public SampleRelationshipHandler(
      @Qualifier("WEBINCLIENT") final BioSamplesClient bioSamplesWebinClient,
      final BioSamplesClient bioSamplesAapClient) {
    this.bioSamplesWebinClient = bioSamplesWebinClient;
    this.bioSamplesAapClient = bioSamplesAapClient;
  }

  public List<SpreadsheetRow> parseSpreadsheet(final String filePath) throws IOException {
    final List<SpreadsheetRow> rows = new ArrayList<>();

    try (final FileInputStream fis = new FileInputStream(filePath);
        final Workbook workbook = WorkbookFactory.create(fis)) {
      final Sheet sheet = workbook.getSheetAt(0);
      final Iterator<Row> iterator = sheet.iterator();

      // Skip the header row
      if (iterator.hasNext()) {
        iterator.next();
      }

      while (iterator.hasNext()) {
        final Row currentRow = iterator.next();

        final String id = currentRow.getCell(0).getStringCellValue();
        final String alias = currentRow.getCell(1).getStringCellValue();
        final String title = currentRow.getCell(2).getStringCellValue();
        final String parentID = currentRow.getCell(3).getStringCellValue();
        final String childID = currentRow.getCell(4).getStringCellValue();
        final String secondaryIdPlantChild = currentRow.getCell(5).getStringCellValue();
        final String secondaryIdSoilParent = currentRow.getCell(6).getStringCellValue();

        final SpreadsheetRow row =
            new SpreadsheetRow(
                id, alias, title, parentID, childID, secondaryIdPlantChild, secondaryIdSoilParent);
        rows.add(row);
      }
    } catch (final Exception e) {
      throw e;
    }

    return rows;
  }

  public void processFile(final String filePath) throws IOException {
    final List<SpreadsheetRow> rows = parseSpreadsheet(filePath);

    for (final SpreadsheetRow row : rows) {
      log.info("Handling " + row.id);

      final String targetSampleAccession = row.secondaryIdSoilParent;
      final String sourceSampleAccession = row.secondaryIdPlantChild;
      final Optional<EntityModel<Sample>> sourceSampleOptional =
          bioSamplesWebinClient.fetchSampleResource(sourceSampleAccession, false);

      if (sourceSampleOptional.isPresent()) {
        final Sample sourceSample = sourceSampleOptional.get().getContent();
        final Relationship relationship =
            Relationship.build(sourceSampleAccession, "derived from", targetSampleAccession);
        final Set<Relationship> relationships = sourceSample.getRelationships();

        relationships.add(relationship);

        final Sample updatedSourceSample =
            Sample.Builder.fromSample(sourceSample).withRelationships(relationships).build();

        if (sourceSample.getWebinSubmissionAccountId() != null) {
          bioSamplesWebinClient.persistSampleResource(updatedSourceSample);

          log.info("Updated " + row.id + " with relationships");
        } else {
          bioSamplesAapClient.persistSampleResource(updatedSourceSample);

          log.info("Updated " + row.id + " with relationships");
        }
      }
    }
  }

  @Data
  @AllArgsConstructor
  public static class SpreadsheetRow {
    private String id;
    private String alias;
    private String title;
    private String parentID;
    private String childID;
    private String secondaryIdPlantChild;
    private String secondaryIdSoilParent;
  }
}
