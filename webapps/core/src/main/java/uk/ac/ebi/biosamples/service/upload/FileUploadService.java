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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
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
import uk.ac.ebi.biosamples.service.SampleService;
import uk.ac.ebi.biosamples.service.SchemaValidationService;
import uk.ac.ebi.biosamples.service.certification.CertifyService;
import uk.ac.ebi.biosamples.utils.upload.Characteristics;
import uk.ac.ebi.biosamples.utils.upload.FileUploadIsaTabUtils;
import uk.ac.ebi.biosamples.utils.upload.ValidationResult;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
public class FileUploadService {
  private Logger log = LoggerFactory.getLogger(getClass());
  private FileUploadIsaTabUtils fileUploadIsaTabUtils;
  private ValidationResult validationResult;

  @Autowired ObjectMapper objectMapper;

  @Autowired SampleService sampleService;

  @Autowired CertifyService certifyService;

  @Autowired SchemaValidationService schemaValidationService;

  @Autowired JsonSchemaStoreAccessibilityCheckService jsonSchemaStoreAccessibilityCheckService;

  public synchronized File upload(
      MultipartFile file, String aapDomain, String certificate, String webinId) {
    validationResult = new ValidationResult();
    fileUploadIsaTabUtils = new FileUploadIsaTabUtils();

    try {
      Path temp = Files.createTempFile("upload", ".tsv");

      File fileToBeUploaded = temp.toFile();
      file.transferTo(fileToBeUploaded);

      log.info("Input file name " + fileToBeUploaded.getName());

      FileReader fr = new FileReader(fileToBeUploaded);
      BufferedReader reader = new BufferedReader(fr);

      final CSVParser csvParser = fileUploadIsaTabUtils.buildParser(reader);
      final List<String> headers = csvParser.getHeaderNames();
      final List<Multimap<String, String>> csvDataMap = fileUploadIsaTabUtils.getCSVDataInMap(csvParser);
      log.info("CSV data size: " + csvDataMap.size());

      final List<Sample> samples = buildSamples(csvDataMap, aapDomain, webinId, certificate, validationResult);

      final String persistenceMessage = "Number of samples persisted: " + samples.size();

      log.info("Persistance message: " + persistenceMessage);
      validationResult.addValidationMessage(persistenceMessage);
      log.info("Final message: " + String.join("\n", validationResult.getValidationMessagesList()));

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

  private File writeToFile(File fileToBeUploaded, List<String> headers, List<Sample> samples) {
    try {
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
    } catch (final Exception e) {
      log.info("Writing to file has failed " + e.getMessage(), e);

      e.printStackTrace();
      return null;
    }
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
          String certificate,
          ValidationResult validationResult) {
    final Map<String, String> sampleNameToAccessionMap = new LinkedHashMap<>();
    final Map<Sample, Multimap<String, String>> sampleToMappedSample = new LinkedHashMap<>();
    final boolean isSchemaValidatorAccessible =
        jsonSchemaStoreAccessibilityCheckService.checkJsonSchemaStoreConnectivity();

    csvDataMap.forEach(
        csvRecordMap -> {
          Sample sample = null;

          try {
            sample =
                buildSample(
                    csvRecordMap, aapDomain, webinId, certificate, isSchemaValidatorAccessible);

            if (sample == null) {
              this.validationResult.addValidationMessage("Failed to create all samples in the file");
            }
          } catch (Exception e) {
            this.validationResult.addValidationMessage("Failed to create all samples in the file");
          }

          if (sample != null) {
            sampleNameToAccessionMap.put(sample.getName(), sample.getAccession());
            sampleToMappedSample.put(sample, csvRecordMap);
          }
        });

    return addRelationshipsAndThenBuildSamples(
        sampleNameToAccessionMap, sampleToMappedSample, webinId, validationResult);
  }

  private boolean isWebinIdUsedToAuthenticate(String webinId) {
    return webinId != null && webinId.toUpperCase().startsWith("WEBIN");
  }

  private List<Sample> addRelationshipsAndThenBuildSamples(
          Map<String, String> sampleNameToAccessionMap,
          Map<Sample, Multimap<String, String>> sampleToMappedSample,
          String webinId, ValidationResult validationResult) {
    return sampleToMappedSample.entrySet().stream()
        .map(
            sampleMultimapEntry ->
                addRelationshipAndThenBuildSample(
                    sampleNameToAccessionMap, sampleMultimapEntry, webinId, validationResult))
        .collect(Collectors.toList());
  }

  private Sample addRelationshipAndThenBuildSample(
          Map<String, String> sampleNameToAccessionMap,
          Map.Entry<Sample, Multimap<String, String>> sampleMultimapEntry,
          String webinId, ValidationResult validationResult) {
    final String authProvider = isWebinIdUsedToAuthenticate(webinId) ? "WEBIN" : "AAP";
    final Multimap<String, String> relationshipMap =
        fileUploadIsaTabUtils.parseRelationships(sampleMultimapEntry.getValue());
    Sample sample = sampleMultimapEntry.getKey();
    final List<Relationship> relationships =
        fileUploadIsaTabUtils.createRelationships(sample, sampleNameToAccessionMap, relationshipMap, validationResult);

    relationships.forEach(relationship -> log.info(relationship.toString()));

    sample = Sample.Builder.fromSample(sample).withRelationships(relationships).build();
    sample = sampleService.store(sample, true, authProvider);

    return sample;
  }

  private Sample buildSample(
          Multimap<String, String> multiMap,
          String aapDomain,
          String webinId,
          String certificate,
          boolean isSchemaValidatorAccessible)
      throws JsonProcessingException {
    final String sampleName = fileUploadIsaTabUtils.getSampleName(multiMap);
    final String sampleReleaseDate = fileUploadIsaTabUtils.getReleaseDate(multiMap);
    final String accession = fileUploadIsaTabUtils.getSampleAccession(multiMap);
    final List<Characteristics> characteristicsList = fileUploadIsaTabUtils.handleCharacteristics(multiMap);
    final List<ExternalReference> externalReferenceList = fileUploadIsaTabUtils.handleExternalReferences(multiMap);
    final List<Contact> contactsList = fileUploadIsaTabUtils.handleContacts(multiMap);
    final String authProvider = isWebinIdUsedToAuthenticate(webinId) ? "WEBIN" : "AAP";
    boolean basicValidationFailure = false;
    boolean isCertified;

    if (sampleName == null || sampleName.isEmpty()) {
      this.validationResult.addValidationMessage(
          "All samples in the file must have a sample name, some samples are missing sample name and hence are not created");
      basicValidationFailure = true;
    }

    if (sampleReleaseDate == null || sampleReleaseDate.isEmpty()) {
      this.validationResult.addValidationMessage(
          "All samples in the file must have a release date "
              + sampleName
              + " doesn't have a release date and is not created");
      basicValidationFailure = true;
    }

    if (!basicValidationFailure) {
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

      final Set<Attribute> attributeSet = sample.getAttributes();
      final Attribute attribute =
          new Attribute.Builder("checklist", certificate.substring(0, certificate.indexOf('(')))
              .build();

      attributeSet.add(attribute);
      sample = Sample.Builder.fromSample(sample).withAttributes(attributeSet).build();

      try {
        if (isSchemaValidatorAccessible) {
          schemaValidationService.validate(sample);
          isCertified = true;
        } else {
          log.info("Schema validator not accesible, trying certification service");
          isCertified = certify(sample, certificate);
        }
      } catch (final Exception schemaValidationException) {
        log.info("Schema validator failed to validate sample, trying certification service");
        isCertified = certify(sample, certificate);
      }

      if (isCertified) {
        sample = sampleService.store(sample, false, true, authProvider);
        log.info("Sample " + sample.getName() + " created with accession " + sample.getAccession());

        return sample;
      } else {
        this.validationResult.addValidationMessage(
            sampleName + " failed validation against " + certificate);

        return null;
      }
    } else {
      return null;
    }
  }

  private boolean certify(Sample sample, String certificate) throws JsonProcessingException {
    List<Certificate> certificates =
        certifyService.certify(objectMapper.writeValueAsString(sample), false, certificate);

    return certificates.size() != 0;
  }
}
