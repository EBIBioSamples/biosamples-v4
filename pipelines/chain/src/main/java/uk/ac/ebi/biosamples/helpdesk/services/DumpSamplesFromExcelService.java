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

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.util.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** Reads sampleId values from an Excel sheet and dumps fetched samples as JSON lines. */
@Service
public class DumpSamplesFromExcelService {
  private static final Logger log = LoggerFactory.getLogger(DumpSamplesFromExcelService.class);

  private static final int BATCH_SIZE = 100;
  private static final int MAX_RETRIES = 5;
  // POI default max record is 100,000,000. Large spreadsheets can exceed this.
  private static final int POI_MAX_RECORD_OVERRIDE = 300_000_000;

  private final SampleJsonDumpService sampleJsonDumpService;

  public DumpSamplesFromExcelService(final SampleJsonDumpService sampleJsonDumpService) {
    this.sampleJsonDumpService = sampleJsonDumpService;
  }

  /**
   * Reads the sampleId column from the first sheet, fetches samples in batches of 1000 using V2,
   * and writes full sample JSON lines to output.
   *
   * @param excelFilePath source Excel file path
   * @param outputFilePath output JSONL file path
   */
  public void dumpFromExcelSampleIdColumn(final String excelFilePath, final String outputFilePath) {
    long readRows = 0;
    long queuedAccessions = 0;
    long writtenSamples = 0;
    final DataFormatter formatter = new DataFormatter();
    final List<String> batch = new ArrayList<>(BATCH_SIZE);

    // Raise POI record size limit for very large XLSX files.
    IOUtils.setByteArrayMaxOverride(POI_MAX_RECORD_OVERRIDE);

    try (FileInputStream fis = new FileInputStream(excelFilePath);
        Workbook workbook = WorkbookFactory.create(fis);
        BufferedWriter writer =
            Files.newBufferedWriter(
                Paths.get(outputFilePath),
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING)) {
      final Sheet sheet = workbook.getSheetAt(0);
      final Row header = sheet.getRow(sheet.getFirstRowNum());
      if (header == null) {
        throw new IllegalArgumentException("Excel sheet has no header row");
      }

      final int sampleIdCol = findColumnIndex(header, formatter, "SAMPLE_ID");
      if (sampleIdCol < 0) {
        throw new IllegalArgumentException("Column 'SAMPLE_ID' not found in header row");
      }

      for (int r = sheet.getFirstRowNum() + 1; r <= sheet.getLastRowNum(); r++) {
        final Row row = sheet.getRow(r);
        if (row == null) {
          continue;
        }
        readRows++;

        final String accession = getCellValue(row.getCell(sampleIdCol), formatter).trim();
        if (accession.isEmpty()) {
          continue;
        }

        batch.add(accession);
        queuedAccessions++;

        if (batch.size() >= BATCH_SIZE) {
          writtenSamples +=
              sampleJsonDumpService.writeSamplesByAccessionBatchWithRetry(
                  batch, writer, MAX_RETRIES);
          batch.clear();
        }

        if (readRows % 10000 == 0) {
          log.info(
              "Progress: read {} rows, queued {} accessions, written {} samples",
              readRows,
              queuedAccessions,
              writtenSamples);
        }
      }

      if (!batch.isEmpty()) {
        writtenSamples +=
            sampleJsonDumpService.writeSamplesByAccessionBatchWithRetry(batch, writer, MAX_RETRIES);
      }
    } catch (IOException e) {
      throw new RuntimeException("Failed processing Excel file: " + excelFilePath, e);
    }

    log.info(
        "Completed Excel dump: read {} rows, queued {} accessions, wrote {} samples to {}",
        readRows,
        queuedAccessions,
        writtenSamples,
        outputFilePath);
  }

  private static int findColumnIndex(
      final Row header, final DataFormatter formatter, final String columnName) {
    final short first = header.getFirstCellNum();
    final short last = header.getLastCellNum();
    for (int c = first; c < last; c++) {
      final String value = getCellValue(header.getCell(c), formatter).trim();
      if (value.equalsIgnoreCase(columnName)) {
        return c;
      }
    }
    return -1;
  }

  private static String getCellValue(final Cell cell, final DataFormatter formatter) {
    return cell == null ? "" : formatter.formatCellValue(cell);
  }
}
