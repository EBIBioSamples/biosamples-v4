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
package uk.ac.ebi.biosamples.misc;

import org.springframework.stereotype.Component;

@Component
public class RTHandler2 {
  /*private static final Logger log = LoggerFactory.getLogger(RTHandler2.class);
    public static final String RELS_DERIVED_FROM = "derived from";
    private final BioSamplesClient bioSamplesWebinClient;
    private final PipelinesProperties pipelinesProperties;

    public RTHandler2(
        @Qualifier("WEBINCLIENT") final BioSamplesClient bioSamplesWebinClient,
        final PipelinesProperties pipelinesProperties) {
      this.bioSamplesWebinClient = bioSamplesWebinClient;
      this.pipelinesProperties = pipelinesProperties;
    }

    public void parseFileAndAddRelationshipCurationToSample() {
      final String filePath = "C:\\Users\\dgupta\\DIME_BSD.xlsx";
      final List<String> curationDomainBlankList = new ArrayList<>();

      curationDomainBlankList.add("");

      try {
        final List<SampleObject> samples = parseSpreadsheet(filePath);

        // Now, 'samples' list contains objects for each row.
        for (final SampleObject sample : samples) {
          final String accession = sample.getAccession();

          log.info("Processing Sample: " + accession);

          final Curation curation =
              Curation.build(
                  null,
                  null,
                  null,
                  null,
                  null,
                  List.of(Relationship.build(accession, RELS_DERIVED_FROM, sample.getDerivedFrom())));

          bioSamplesWebinClient.persistCuration(
              sample.getAccession(), curation, pipelinesProperties.getProxyWebinId(), true);
        }
      } catch (final IOException e) {
        e.printStackTrace();
      }
    }

    public static List<SampleObject> parseSpreadsheet(final String filePath) throws IOException {
      final List<SampleObject> samples = new ArrayList<>();
      final FileInputStream file = new FileInputStream(new File(filePath));
      final XSSFWorkbook workbook = new XSSFWorkbook(file);
      final Iterator<Row> rowIterator = workbook.getSheetAt(0).iterator();

      // Skip header row
      if (rowIterator.hasNext()) {
        rowIterator.next();
      }

      while (rowIterator.hasNext()) {
        final Row row = rowIterator.next();

        // Check if all cells in the row are blank
        if (isRowBlank(row)) {
          continue; // Skip this row
        }

        final SampleObject sample = createSampleObjectFromRow(row);
        samples.add(sample);
      }

      workbook.close();
      file.close();

      return samples;
    }

    public static boolean isRowBlank(final Row row) {
      final Iterator<Cell> cellIterator = row.iterator();
      while (cellIterator.hasNext()) {
        final Cell cell = cellIterator.next();
        if (cell.getCellType() != CellType.BLANK) {
          return false; // The row is not blank if any cell is not blank
        }
      }

      return true; // All cells are blank
    }

    public static SampleObject createSampleObjectFromRow(final Row row) {
      final SampleObject sample = new SampleObject();

      sample.setSourceName(getStringValue(row.getCell(0)));
      sample.setSampleName(getStringValue(row.getCell(1)));
      sample.setAccession(getStringValue(row.getCell(6)));
      sample.setDerivedFrom(getStringValue(row.getCell(7)));
      // Continue setting other properties based on column index

      return sample;
    }

    public static String getStringValue(final Cell cell) {
      return cell == null ? null : cell.getStringCellValue();
    }
  }

  @Data
  class SampleObject {
    private String sourceName;
    private String sampleName;
    private String accession;
    private String derivedFrom;

    @Override
    public String toString() {
      return "SampleObject{"
          + "sourceName='"
          + sourceName
          + '\''
          + ", sampleName='"
          + sampleName
          + '\''
          + ", accession='"
          + accession
          + '\''
          + ", derivedFrom='"
          + derivedFrom
          + '\''
          + '}';
    }*/
}
