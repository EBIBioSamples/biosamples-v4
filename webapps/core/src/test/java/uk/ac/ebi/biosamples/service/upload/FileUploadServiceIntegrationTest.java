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
package uk.ac.ebi.biosamples.service.upload;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.ExternalReference;
import uk.ac.ebi.biosamples.model.Relationship;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.utils.upload.Characteristics;

public class FileUploadServiceIntegrationTest {
  public static void main(String[] args) throws IOException {
    byte[] byArray = downloadExampleFile();
    final Logger log = LoggerFactory.getLogger("test_logger");
    final List<Multimap<String, String>> csvDataMap = new ArrayList<>();
    final Map<String, String> sampleNameToAccessionMap = new LinkedHashMap<>();
    final Path path = Paths.get("C:\\Users\\dgupta\\s_MTBLS1_small.txt");
    final File file = path.toFile();

    FileReader fr = new FileReader(file);
    BufferedReader reader = new BufferedReader(fr);
    final CSVParser csvParser = buildParser(reader);
    final List<String> headers = csvParser.getHeaderNames();

    csvParser
        .getRecords()
        .forEach(
            csvRecord -> {
              final AtomicInteger i = new AtomicInteger(0);
              final Multimap<String, String> listMultiMap = LinkedListMultimap.create();

              headers.forEach(
                  header -> {
                    String record = csvRecord.get(i.get());
                    listMultiMap.put(header, record);
                    i.getAndIncrement();
                  });

              csvDataMap.add(listMultiMap);
            });

    buildSamples(csvDataMap, sampleNameToAccessionMap);

    // write to file
    final Reader in = new FileReader(file);

    final List<String> outputFileHeaders = new ArrayList<>(headers);

    // outputFileHeaders.add("Sample Identifier");

    String[] headerParsed = headers.toArray(new String[headers.size() + 2]);
    headerParsed[headers.size() + 1] = "Sample Identifier";

    log.info("Writing to file");

    Iterable<CSVRecord> records =
        CSVFormat.TDF
            .withHeader(headerParsed)
            .withAllowDuplicateHeaderNames()
            .withFirstRecordAsHeader()
            .withIgnoreEmptyLines()
            .withIgnoreHeaderCase()
            .withAllowMissingColumnNames()
            .withIgnoreSurroundingSpaces()
            .withTrim()
            .parse(in);

    final Path pathToWrite = Paths.get("C:\\Users\\dgupta\\output1.txt");

    try (final BufferedWriter writer =
            Files.newBufferedWriter(pathToWrite, StandardCharsets.UTF_8);
        final CSVPrinter csvPrinter =
            new CSVPrinter(writer, CSVFormat.TDF.withHeader(headerParsed))) {
      for (CSVRecord row : records) {
        csvPrinter.printRecord(getListFromIterator(row.iterator()));
      }
    }
  }

  public static <T> List<T> getListFromIterator(Iterator<T> iterator) {
    Iterable<T> iterable = () -> iterator;

    return StreamSupport.stream(iterable.spliterator(), false).collect(Collectors.toList());
  }

  private static List<Sample> buildSamples(
      List<Multimap<String, String>> csvDataMap, Map<String, String> sampleNameToAccessionMap) {
    csvDataMap.forEach(csvRecordMap -> buildSample(csvRecordMap, sampleNameToAccessionMap));
    return null;
  }

  private static void buildSample(
      Multimap<String, String> multiMap, Map<String, String> sampleNameToAccessionMap) {
    final String sampleName = getSampleName(multiMap);
    final List<Characteristics> characteristicsList = handleCharacteristics(multiMap);
    final List<ExternalReference> externalReferenceList = handleExternalReferences(multiMap);
    final Sample sample =
        new Sample.Builder(sampleName)
            .withAttributes(
                characteristicsList.stream()
                    .map(
                        characteristics -> {
                          Attribute attribute =
                              new Attribute.Builder(
                                      characteristics.getName(), characteristics.getValue())
                                  .withTag("attribute")
                                  .withUnit(characteristics.getUnit())
                                  .withIri(characteristics.getIri())
                                  .build();

                          return attribute;
                        })
                    .collect(Collectors.toList()))
            .build();
    sampleNameToAccessionMap.put(sample.getName(), "SAMEA100033");

    final Map<String, String> relationshipMap = parseRelationships(multiMap);

    System.out.println(characteristicsList.size());
  }

  private static List<ExternalReference> handleExternalReferences(
      Multimap<String, String> multiMap) {
    List<ExternalReference> externalReferenceList = new ArrayList<>();

    multiMap
        .entries()
        .forEach(
            entry -> {
              final String entryKey = entry.getKey();
              final String entryValue = entry.getValue();

              if (entryKey.startsWith("Comment") && entryKey.contains("bsd_relationship")) {
                externalReferenceList.add(ExternalReference.build(entry.getValue()));
              }
            });

    return externalReferenceList;
  }

  private static List<Relationship> handleRelationships(Multimap<String, String> multiMap) {
    final List<Relationship> relationshipsList = new ArrayList<>();

    multiMap.entries().stream()
        .map(Map.Entry::getKey)
        .filter(
            key -> {
              if (key.startsWith("Comment") && key.contains("bsd_relationship")) {
                Relationship relationship =
                    Relationship.build(
                        "SAMEA100033",
                        key.substring(key.indexOf("bsd_relationship:")),
                        "ADG10003u_008");
                relationshipsList.add(relationship);
              }

              return false;
            });

    return relationshipsList;
  }

  private static Map<String, String> parseRelationships(Multimap<String, String> multiMap) {
    return multiMap.entries().stream()
        .filter(
            entry ->
                entry.getKey().startsWith("Comment") && entry.getKey().contains("bsd_relationship"))
        .collect(
            Collectors.toMap(
                e -> {
                  final String key = e.getKey();
                  return key.substring(key.indexOf(":") + 1, key.length() - 1);
                },
                e -> (String) e.getValue(),
                (u, v) -> u,
                LinkedHashMap::new));
  }

  private static List<Characteristics> handleCharacteristics(Multimap<String, String> multiMap) {
    final List<Characteristics> characteristicsList = new ArrayList<>();

    multiMap
        .entries()
        .forEach(
            entry -> {
              final Characteristics characteristics = new Characteristics();
              if (entry.getKey().startsWith("Characteristics")) {
                characteristics.setName(entry.getKey());
                characteristics.setValue(entry.getValue());

                characteristicsList.add(characteristics);
              }
            });

    List<String> termRefList =
        multiMap.entries().stream()
            .map(
                entry -> {
                  if (entry.getKey().startsWith("Term Accession Number")) {
                    return entry.getValue();
                  } else return null;
                })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

    List<String> unitList =
        multiMap.entries().stream()
            .map(
                entry -> {
                  if (entry.getKey().startsWith("Unit")) {
                    return entry.getValue();
                  } else return null;
                })
            .filter(Objects::nonNull)
            .collect(Collectors.toList()); // handle units

    AtomicInteger i = new AtomicInteger(0);

    characteristicsList.forEach(
        characteristics -> {
          final int val = i.getAndIncrement();

          if (val < termRefList.size() && termRefList.get(val) != null) {
            characteristics.setIri(termRefList.get(val));
          }

          if (val < unitList.size() && unitList.get(val) != null) {
            characteristics.setUnit(unitList.get(val));
          }
        });

    return characteristicsList;
  }

  private static String getSampleName(Multimap<String, String> multiMap) {
    Optional<String> sampleName = multiMap.get("Sample Name").stream().findFirst();

    // throw here
    return sampleName.orElse(null);
  }

  private static CSVParser buildParser(BufferedReader reader) throws IOException {
    return new CSVParser(
        reader,
        CSVFormat.TDF
            .withAllowDuplicateHeaderNames()
            .withFirstRecordAsHeader()
            .withIgnoreEmptyLines()
            .withIgnoreHeaderCase()
            .withAllowMissingColumnNames()
            .withIgnoreSurroundingSpaces()
            .withTrim());
  }

  public static byte[] downloadExampleFile() throws IOException {
    final Path temp = Files.createTempFile("upload_example", ".tsv");
    final File pathFile = temp.toFile();
    FileUtils.copyInputStreamToFile(
        FileUploadServiceIntegrationTest.class
            .getClassLoader()
            .getResourceAsStream("isa-example.tsv"),
        pathFile);
    return FileUtils.readFileToByteArray(pathFile);
  }
}
