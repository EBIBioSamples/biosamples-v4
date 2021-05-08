/*
* Copyright 2019 EMBL - European Bioinformatics Institute
* Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
* file except in compliance with the License. You may obtain a copy of the License at
* http://www.apache.org/licenses/LICENSE-2.0
* Unless required by applicable law or agreed to in writing, software distributed under the
* License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
* CONDITIONS OF ANY KIND, either express or implied. See the License for the
* specific language governing permissions and limitations under the License.
*/
package uk.ac.ebi.biosamples.service.upload;

import static com.google.common.collect.Multimaps.toMultimap;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import uk.ac.ebi.biosamples.model.*;
import uk.ac.ebi.biosamples.model.upload.Characteristics;
import uk.ac.ebi.biosamples.model.upload.validation.ValidationResult;
import uk.ac.ebi.biosamples.service.SampleService;
import uk.ac.ebi.biosamples.service.certification.CertifyService;

@Service
public class IsaTabUploadService {
  private Logger log = LoggerFactory.getLogger(getClass());
  private ValidationResult validationResult;

  @Autowired ObjectMapper objectMapper;

  @Autowired SampleService sampleService;

  @Autowired CertifyService certifyService;

  public synchronized File upload(
      MultipartFile file, String aapDomain, String certificate, String webinId) {
    log.info("Upload called");
    validationResult = new ValidationResult();

    try {
      final List<Multimap<String, String>> csvDataMap = new ArrayList<>();
      Path temp = Files.createTempFile("upload", ".tsv");

      File fileToBeUploaded = temp.toFile();
      file.transferTo(fileToBeUploaded);

      log.info("Input file name " + fileToBeUploaded.getName());

      FileReader fr = new FileReader(fileToBeUploaded);
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

      log.info("CSV data size: " + csvDataMap.size());

      final List<Sample> samples = buildSamples(csvDataMap, aapDomain, webinId, certificate);

      final String persistenceMessage = "Number of samples persisted: " + samples.size();

      log.info(persistenceMessage);
      validationResult.addValidationMessage(persistenceMessage);
      log.info(String.join("\n", validationResult.getValidationMessagesList()));

      return writeToFile(fileToBeUploaded, headers, samples);
    } catch (Exception e) {
      final String messageForBsdDevTeam =
          "********FEEDBACK TO BSD DEV TEAM START********"
              + e.getMessage()
              + "********FEEDBACK TO BSD DEV TEAM END********";
      validationResult.addValidationMessage(messageForBsdDevTeam);
      throw new UploadInvalidException(
          String.join("\n", validationResult.getValidationMessagesList()));
    } finally {
      validationResult.clear();
    }
  }

  private <T> List<T> getPrintableListFromCsvRow(Iterator<T> iterator) {
    Iterable<T> iterable = () -> iterator;

    return StreamSupport.stream(iterable.spliterator(), false).collect(Collectors.toList());
  }

  private File writeToFile(File fileToBeUploaded, List<String> headers, List<Sample> samples)
      throws IOException {
    log.info("Writing to file");
    Path temp = Files.createTempFile("upload_result", ".tsv");
    boolean headerHasIdentifier =
        headers.stream().anyMatch(header -> header.equalsIgnoreCase("Sample Identifier"));

    final List<String> outputFileHeaders = new ArrayList<>(headers);

    if (!headerHasIdentifier) {
      outputFileHeaders.add("Sample Identifier");
    }
    final Reader in = new FileReader(fileToBeUploaded);
    final String[] headerParsed = outputFileHeaders.toArray(new String[outputFileHeaders.size()]);

    final Iterable<CSVRecord> records =
        CSVFormat.TDF
            .withHeader(headerParsed)
            .withFirstRecordAsHeader()
            .withAllowDuplicateHeaderNames()
            .withFirstRecordAsHeader()
            .withIgnoreEmptyLines()
            .withIgnoreHeaderCase()
            .withAllowMissingColumnNames()
            .withIgnoreSurroundingSpaces()
            .withTrim()
            .parse(in);

    try (final BufferedWriter writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8);
        final CSVPrinter csvPrinter =
            new CSVPrinter(writer, CSVFormat.TDF.withHeader(headerParsed));
        final PrintWriter out = new PrintWriter(writer)) {
      for (CSVRecord row : records) {
        if (headerHasIdentifier) {
          csvPrinter.printRecord(row);
        } else {
          csvPrinter.printRecord(
              addAccessionToSamplesForPrint(getPrintableListFromCsvRow(row.iterator()), samples));
        }
      }

      out.println("\n\n");
      out.println("********RECEIPT START********");
      out.println(String.join("\n", validationResult.getValidationMessagesList()));
      out.println("********RECEIPT END********");
      out.println("\n\n");
    }

    return temp.toFile();
  }

  private Iterable<?> addAccessionToSamplesForPrint(
      List<String> listFromIterator, List<Sample> samples) {
    String sampleName = listFromIterator.get(1);
    Optional<Sample> sampleOptional =
        samples.stream().filter(sample -> sample.getName().equals(sampleName)).findFirst();

    sampleOptional.ifPresent(sample -> listFromIterator.add(sample.getAccession()));

    return listFromIterator;
  }

  private List<Sample> buildSamples(
      List<Multimap<String, String>> csvDataMap,
      String aapDomain,
      String webinId,
      String certificate) {
    final Map<String, String> sampleNameToAccessionMap = new LinkedHashMap<>();
    final Map<Sample, Multimap<String, String>> sampleToMappedSample = new LinkedHashMap<>();

    csvDataMap.forEach(
        csvRecordMap -> {
          Sample sample = null;

          try {
            sample = buildSample(csvRecordMap, aapDomain, webinId, certificate);

            if (sample == null) {
              validationResult.addValidationMessage("Failed to create all samples in the file");
            }
          } catch (Exception e) {
            validationResult.addValidationMessage("Failed to create all samples in the file");
          }

          if (sample != null) {
            sampleNameToAccessionMap.put(sample.getName(), sample.getAccession());
            sampleToMappedSample.put(sample, csvRecordMap);
          }
        });

    return addRelationshipsAndThenBuildSamples(
        sampleNameToAccessionMap, sampleToMappedSample, webinId);
  }

  private boolean isWebinIdUsedToAuthenticate(String webinId) {
    return webinId != null && webinId.toUpperCase().startsWith("WEBIN");
  }

  private List<Sample> addRelationshipsAndThenBuildSamples(
      Map<String, String> sampleNameToAccessionMap,
      Map<Sample, Multimap<String, String>> sampleToMappedSample,
      String webinId) {
    return sampleToMappedSample.entrySet().stream()
        .map(
            sampleMultimapEntry ->
                addRelationshipAndThenBuildSample(
                    sampleNameToAccessionMap, sampleMultimapEntry, webinId))
        .collect(Collectors.toList());
  }

  private Sample addRelationshipAndThenBuildSample(
      Map<String, String> sampleNameToAccessionMap,
      Map.Entry<Sample, Multimap<String, String>> sampleMultimapEntry,
      String webinId) {
    final String authProvider = isWebinIdUsedToAuthenticate(webinId) ? "WEBIN" : "AAP";
    final Multimap<String, String> relationshipMap =
        parseRelationships(sampleMultimapEntry.getValue());
    Sample sample = sampleMultimapEntry.getKey();
    final List<Relationship> relationships =
        createRelationships(sample, sampleNameToAccessionMap, relationshipMap);

    relationships.forEach(relationship -> log.info(relationship.toString()));

    sample = Sample.Builder.fromSample(sample).withRelationships(relationships).build();
    sample = sampleService.store(sample, false, true, authProvider);

    return sample;
  }

  private Sample buildSample(
      Multimap<String, String> multiMap, String aapDomain, String webinId, String certificate)
      throws JsonProcessingException {
    final String sampleName = getSampleName(multiMap);
    final String sampleReleaseDate = getReleaseDate(multiMap);
    final String accession = getSampleAccession(multiMap);
    final List<Characteristics> characteristicsList = handleCharacteristics(multiMap);
    final List<ExternalReference> externalReferenceList = handleExternalReferences(multiMap);
    final List<Contact> contactsList = handleContacts(multiMap);
    final String authProvider = isWebinIdUsedToAuthenticate(webinId) ? "WEBIN" : "AAP";
    boolean validationFailure = false;

    if (sampleName == null || sampleName.isEmpty()) {
      validationResult.addValidationMessage(
          "All samples in the file must have a sample name, some samples are missing sample name and hence are not created");
      validationFailure = true;
    }

    if (sampleReleaseDate == null || sampleReleaseDate.isEmpty()) {
      validationResult.addValidationMessage(
          "All samples in the file must have a release date "
              + sampleName
              + " doesn't have a release date and is not created");
      validationFailure = true;
    }

    if (!validationFailure) {
      Sample sample =
          new Sample.Builder(sampleName.trim())
              .withAccession(accession)
              .withAttributes(
                  characteristicsList.stream()
                      .map(
                          characteristics -> {
                            final String name = characteristics.getName();

                            return new Attribute.Builder(
                                    name.substring(name.indexOf('[') + 1, name.indexOf(']')),
                                    characteristics.getValue())
                                .withTag("attribute")
                                .withUnit(characteristics.getUnit())
                                .withIri(characteristics.getIri())
                                .build();
                          })
                      .collect(Collectors.toList()))
              .withExternalReferences(externalReferenceList)
              .withContacts(contactsList)
              .build();

      if (sampleService.isWebinAuthorization(authProvider)) {
        sample = Sample.Builder.fromSample(sample).withWebinSubmissionAccountId(webinId).build();
      } else {
        sample = Sample.Builder.fromSample(sample).withDomain(aapDomain).build();
      }

      final boolean isCertified = certify(sample, certificate);

      if (isCertified) {
        final Set<Attribute> attributeSet = sample.getAttributes();
        final Attribute attribute = new Attribute.Builder("checklist", certificate).build();

        attributeSet.add(attribute);
        sample = Sample.Builder.fromSample(sample).withAttributes(attributeSet).build();
        sample = sampleService.store(sample, false, true, authProvider);
      }

      log.info("Sample " + sample.getName() + " created with accession " + sample.getAccession());

      return sample;
    } else {
      return null;
    }
  }

  private List<Contact> handleContacts(Multimap<String, String> multiMap) {
    List<Contact> contactList = new ArrayList<>();

    multiMap
        .entries()
        .forEach(
            entry -> {
              final String entryKey = entry.getKey();
              final String entryValue = entry.getValue();

              log.info(entryKey + " " + entryValue);

              if (entryKey.startsWith("Comment") && entryKey.contains("submission_contact")) {
                contactList.add(new Contact.Builder().email(entry.getValue()).build());
              }
            });

    return contactList;
  }

  private String getSampleAccession(Multimap<String, String> multiMap) {
    Optional<String> sampleAccession = multiMap.get("Sample Identifier").stream().findFirst();

    return sampleAccession.orElse(null);
  }

  private List<ExternalReference> handleExternalReferences(Multimap<String, String> multiMap) {
    List<ExternalReference> externalReferenceList = new ArrayList<>();

    multiMap
        .entries()
        .forEach(
            entry -> {
              final String entryKey = entry.getKey();
              final String entryValue = entry.getValue();

              log.info(entryKey + " " + entryValue);

              if (entryKey.startsWith("Comment") && entryKey.contains("external DB REF")) {
                externalReferenceList.add(ExternalReference.build(entry.getValue()));
              }
            });

    return externalReferenceList;
  }

  private List<Relationship> createRelationships(
      Sample sample,
      Map<String, String> sampleNameToAccessionMap,
      Multimap<String, String> relationshipMap) {
    return relationshipMap.entries().stream()
        .map(entry -> getRelationship(sample, sampleNameToAccessionMap, entry))
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }

  private Relationship getRelationship(
      Sample sample,
      Map<String, String> sampleNameToAccessionMap,
      Map.Entry<String, String> entry) {
    try {
      final String relationshipTarget = getRelationshipTarget(sampleNameToAccessionMap, entry);

      if (relationshipTarget != null) {
        return Relationship.build(sample.getAccession(), entry.getKey().trim(), relationshipTarget);
      } else {
        validationResult.addValidationMessage(
            "Failed to add all relationships for " + sample.getAccession());
        return null;
      }
    } catch (Exception e) {
      log.info("Failed to add relationship");
      validationResult.addValidationMessage(
          "Failed to add all relationships for "
              + sample.getAccession()
              + " error: "
              + e.getMessage());
      return null;
    }
  }

  private String getRelationshipTarget(
      Map<String, String> sampleNameToAccessionMap, Map.Entry<String, String> entry) {
    final String relationshipTarget = entry.getValue().trim();

    if (relationshipTarget.startsWith("SAM")) {
      return relationshipTarget;
    } else {
      return sampleNameToAccessionMap.get(relationshipTarget);
    }
  }

  private boolean certify(Sample sample, String certificate) throws JsonProcessingException {
    List<Certificate> certificates =
        certifyService.certify(objectMapper.writeValueAsString(sample), false, certificate);

    return certificates.size() != 0;
  }

  private Multimap<String, String> parseRelationships(Multimap<String, String> multiMap) {

    return multiMap.entries().stream()
        .filter(
            e -> {
              final String entryKey = e.getKey();

              return entryKey.startsWith("Comment") && entryKey.contains("bsd_relationship");
            })
        .collect(
            toMultimap(
                e -> {
                  final String key = e.getKey().trim();
                  return key.substring(key.indexOf(":") + 1, key.length() - 1);
                },
                e -> {
                  final String value = e.getValue();

                  return value != null ? value.trim() : null;
                },
                LinkedListMultimap::create));

    // BELOW IS PARKED CODE - DON'T DELETE
    /*return
    multiMap.entries().stream()
            .filter(entry -> {
                final String entryKey = entry.getKey();

                return entryKey.startsWith("Comment") && entryKey.contains("bsd_relationship");
            })
            .collect(
                    Collectors.toMap(
                            e -> {
                                final String key = e.getKey().trim();
                                return key.substring(key.indexOf(":") + 1, key.length() - 1);
                            },
                            e -> {
                                final String value = e.getValue();

                                return value != null ? value.trim() : null;
                            },
                            (u, v) -> u,
                            LinkedHashMap::new));*/
  }

  private List<Characteristics> handleCharacteristics(Multimap<String, String> multiMap) {
    final List<Characteristics> characteristicsList = new ArrayList<>();

    multiMap
        .entries()
        .forEach(
            entry -> {
              final Characteristics characteristics = new Characteristics();
              if (entry.getKey().startsWith("Characteristics")) {
                characteristics.setName(entry.getKey().trim());
                final String value = entry.getValue();

                characteristics.setValue(value);

                characteristicsList.add(characteristics);
              }
            });

    List<String> termRefList =
        multiMap.entries().stream()
            .map(
                entry -> {
                  if (entry.getKey().startsWith("Term Accession Number")) {
                    final String value = entry.getValue();

                    return value != null ? value.trim() : null;
                  } else return null;
                })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

    List<String> unitList =
        multiMap.entries().stream()
            .map(
                entry -> {
                  if (entry.getKey().startsWith("Unit")) {
                    final String value = entry.getValue();

                    return value != null ? value.trim() : null;
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

  private String getSampleName(Multimap<String, String> multiMap) {
    Optional<String> sampleName = multiMap.get("Sample Name").stream().findFirst();

    return sampleName.orElse(null);
  }

  private String getReleaseDate(Multimap<String, String> multiMap) {
    Optional<String> sampleReleaseDate = multiMap.get("Release Date").stream().findFirst();

    return sampleReleaseDate.orElse(null);
  }

  private CSVParser buildParser(BufferedReader reader) throws IOException {
    return new CSVParser(
        reader,
        CSVFormat.TDF
            .withAllowDuplicateHeaderNames(true)
            .withFirstRecordAsHeader()
            .withIgnoreEmptyLines(true)
            .withIgnoreHeaderCase(true)
            .withAllowMissingColumnNames(true)
            .withIgnoreSurroundingSpaces(true)
            .withTrim(true));
  }
}
