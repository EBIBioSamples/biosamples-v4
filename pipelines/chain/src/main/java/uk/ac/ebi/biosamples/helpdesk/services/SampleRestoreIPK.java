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

import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.hateoas.EntityModel;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import uk.ac.ebi.biosamples.BioSamplesConstants;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.core.model.Attribute;
import uk.ac.ebi.biosamples.core.model.Sample;

@Service
@Slf4j
public class SampleRestoreIPK {
  private static final String WWWDEV_GET_SAMPLES = "https://wwwdev.ebi.ac.uk/biosamples/samples/";
  // Restore IPK samples
  // 1. Crawl the spreadsheet having 31000 samples
  // 2. Query ERAPRO to check if they are BSD authority, if yes do nothing, record on file
  // 3. If no, fetch the sample from BSD wwwdev, get all attributes
  // 4. Match attribute count in dev and prod, if dev has more attributes,
  // add them to the prod set of attributes, if dev has less than add all to prod
  // 5. Take care not to add SRA accession in dev sample to the prod sample
  // 6. Record all updates in a file
  // 7. Add, publications, organizations from dev sample to prod
  private final BioSamplesClient bioSamplesClient;
  private final RestTemplate restTemplate;

  public SampleRestoreIPK(final BioSamplesClient bioSamplesClient) {
    this.bioSamplesClient = bioSamplesClient;
    this.restTemplate = new RestTemplate();
  }

  public boolean restoreSample(final String accession) {
    log.info("Processing sample " + accession);

    final Sample prodSample =
        bioSamplesClient.fetchSampleResource(accession).map(EntityModel::getContent).orElse(null);
    Set<Attribute> prodSampleAttributes;

    if (prodSample != null) {
      prodSampleAttributes = prodSample.getAttributes();
    } else {
      prodSampleAttributes = new HashSet<>();
    }

    final Sample devSample = getDevSampleAttributes(accession);
    final Set<Attribute> devSampleAttributes = devSample.getAttributes();

    if (devSampleAttributes != null) {
      devSampleAttributes.removeIf(
          attribute -> attribute.getType().equalsIgnoreCase(BioSamplesConstants.SRA_ACCESSION));
    } else {
      log.info("Sample " + accession + " doesn't have any attributes in dev");

      return false;
    }

    if (prodSampleAttributes.stream()
        .anyMatch(attribute -> attribute.getType().equals("ENA-CHECKLIST"))) {
      devSampleAttributes.removeIf(attribute -> attribute.getType().equals("checklist"));
    }

    final Set<Attribute> attributesToAddToProdSample =
        devSampleAttributes.stream()
            .filter(
                attribute ->
                    prodSampleAttributes.stream()
                        .noneMatch(
                            attribute1 ->
                                attribute.getType().equalsIgnoreCase(attribute1.getType())))
            .collect(Collectors.toSet());
    final Set<Attribute> attributesCommon =
        devSampleAttributes.stream()
            .filter(
                attribute ->
                    prodSampleAttributes.stream()
                        .anyMatch(
                            attribute1 ->
                                attribute.getType().equalsIgnoreCase(attribute1.getType())))
            .collect(Collectors.toSet());

    if (!attributesCommon.isEmpty()) {
      prodSampleAttributes.removeIf(
          attribute ->
              attributesCommon.stream()
                  .anyMatch(attribute1 -> attribute.getType().equals(attribute1.getType())));
    }

    prodSampleAttributes.addAll(attributesToAddToProdSample);
    prodSampleAttributes.addAll(attributesCommon);
    prodSample.getContacts().addAll(devSample.getContacts());
    prodSample.getOrganizations().addAll(devSample.getOrganizations());
    prodSample.getRelationships().addAll(devSample.getRelationships());
    prodSample.getPublications().addAll(devSample.getPublications());

    final Sample updatedSample =
        Sample.Builder.fromSample(prodSample).withAttributes(prodSampleAttributes).build();

    bioSamplesClient.persistSampleResource(updatedSample);
    log.info("Updated sample " + accession);

    return true;
  }

  private Sample getDevSampleAttributes(final String accession) {
    return restTemplate.getForEntity(WWWDEV_GET_SAMPLES + accession, Sample.class).getBody();
  }

  public List<String> parseInput(final String filePath) {
    final List<String> accessionsList = new ArrayList<>();

    try (final FileInputStream fis = new FileInputStream(filePath)) {
      try (final BufferedReader br = new BufferedReader(new InputStreamReader(fis))) {
        br.lines().forEach(accessionsList::add);
      }
      /*final Workbook workbook = new XSSFWorkbook(fis);

      final Sheet sheet = workbook.getSheetAt(0);
      sheet
          .rowIterator()
          .forEachRemaining(
              row -> {
                final Cell cell = row.getCell(1);

                if (cell != null && cell.getCellType() == CellType.STRING) {
                  accessionsList.add(cell.getStringCellValue().trim());
                }
              });*/
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    return accessionsList;
  }
}
