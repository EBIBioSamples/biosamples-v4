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
package uk.ac.ebi.biosamples.utils.upload;

import static com.google.common.collect.Multimaps.toMultimap;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.core.model.*;
import uk.ac.ebi.biosamples.exception.GlobalExceptions;

@Service
public class FileUploadUtils {
  private final Logger log = LoggerFactory.getLogger(getClass());
  private static final DateTimeFormatter DATE_TIME_FORMATTER =
      DateTimeFormatter.ofPattern("dd LLL yyyy HH:mm");

  public List<Multimap<String, String>> getISATABDataInMap(final CSVParser csvParser)
      throws IOException {
    final List<Multimap<String, String>> csvDataMap = new ArrayList<>();
    final List<String> headers = csvParser.getHeaderNames();

    csvParser
        .getRecords()
        .forEach(
            csvRecord -> {
              final AtomicInteger i = new AtomicInteger(0);
              final Multimap<String, String> listMultiMap = LinkedListMultimap.create();

              headers.forEach(
                  header -> {
                    final String record = csvRecord.get(i.get());
                    listMultiMap.put(
                        header != null
                                && !(header.startsWith("characteristics")
                                    || header.startsWith("Characteristics"))
                            ? header.toLowerCase()
                            : header,
                        record);
                    i.getAndIncrement();
                  });

              csvDataMap.add(listMultiMap);
            });

    return csvDataMap;
  }

  public Sample buildSample(
      final Multimap<String, String> multiMap, final ValidationResult validationResult) {
    String sampleName = getSampleName(multiMap);
    String sampleReleaseDate = getReleaseDate(multiMap);
    String accession = getSampleAccession(multiMap);
    List<Characteristics> characteristicsList = handleCharacteristics(multiMap);
    List<ExternalReference> externalReferenceList = handleExternalReferences(multiMap);
    List<Contact> contactsList = handleContacts(multiMap);
    List<Publication> publicationsList = handlePublications(multiMap);
    List<Organization> organizationList = handleOrganizations(multiMap);

    if (isValidSample(sampleName, sampleReleaseDate, validationResult)) {
      return buildSample(
          sampleName,
          accession,
          sampleReleaseDate,
          characteristicsList,
          externalReferenceList,
          contactsList,
          publicationsList,
          organizationList);
    }

    return null;
  }

  private List<Organization> handleOrganizations(final Multimap<String, String> multiMap) {
    final List<Organization> organizationsList = new ArrayList<>();
    final List<String> organizationNames = new ArrayList<>();
    final List<String> organizationRoles = new ArrayList<>();
    final List<String> organizationAddresses = new ArrayList<>();
    final List<String> organizationEmails = new ArrayList<>();
    final List<String> organizationUrls = new ArrayList<>();

    multiMap
        .entries()
        .forEach(
            entry -> {
              final String entryKey = entry.getKey();
              final String entryValue = entry.getValue();

              if (entryKey.startsWith("comment[submission_organization:")) {
                if (entryKey.contains("email")) {
                  organizationEmails.add(entryValue);
                }

                if (entryKey.contains("name")) {
                  organizationNames.add(entryValue);
                }

                if (entryKey.contains("address")) {
                  organizationAddresses.add(entryValue);
                }

                if (entryKey.contains("role")) {
                  organizationRoles.add(entryValue);
                }

                if (entryKey.contains("url")) {
                  organizationUrls.add(entryValue);
                }
              }
            });

    /*Contact email is mandatory for the contact information to be built*/
    for (int iter = 0; iter < organizationNames.size(); iter++) {
      final Organization.Builder organizationBuilder = new Organization.Builder();

      organizationBuilder.name(organizationNames.get(iter));

      organizationBuilder.email(
          iter >= organizationEmails.size() ? null : organizationEmails.get(iter));
      organizationBuilder.role(
          iter >= organizationRoles.size() ? null : organizationRoles.get(iter));
      organizationBuilder.address(
          iter >= organizationAddresses.size() ? null : organizationAddresses.get(iter));
      organizationBuilder.url(iter >= organizationUrls.size() ? null : organizationUrls.get(iter));

      if (organizationBuilder.isNotEmpty()) {
        organizationsList.add(organizationBuilder.build());
      }
    }

    return organizationsList;
  }

  private Sample buildSample(
      final String sampleName,
      final String accession,
      final String sampleReleaseDate,
      final List<Characteristics> characteristicsList,
      final List<ExternalReference> externalReferencesList,
      final List<Contact> contactsList,
      final List<Publication> publicationsList,
      final List<Organization> organizationsList) {
    return new Sample.Builder(sampleName.trim())
        .withAccession(accession)
        .withAttributes(
            characteristicsList.stream()
                .map(
                    characteristics -> {
                      final String name = characteristics.getName();
                      final String trimmedCharacteristicsName =
                          name.substring(name.indexOf('[') + 1, name.indexOf(']'));
                      final String characteristicsValue = characteristics.getValue();

                      if (isValidCharacteristics(
                          name, trimmedCharacteristicsName, characteristicsValue)) {

                        return new Attribute.Builder(
                                trimmedCharacteristicsName, characteristicsValue)
                            .withUnit(characteristics.getUnit())
                            .withIri(characteristics.getIri())
                            .build();
                      } else {
                        return null;
                      }
                    })
                .filter(Objects::nonNull)
                .collect(Collectors.toList()))
        .withRelease(sampleReleaseDate)
        .withExternalReferences(externalReferencesList)
        .withContacts(contactsList)
        .withPublications(publicationsList)
        .withOrganizations(organizationsList)
        .withSubmittedVia(SubmittedViaType.FILE_UPLOADER)
        .build();
  }

  private boolean isValidCharacteristics(
      final String name,
      final String trimmedCharacteristicsName,
      final String characteristicsValue) {
    return (name != null && !trimmedCharacteristicsName.isEmpty())
        && (characteristicsValue != null && !characteristicsValue.isEmpty());
  }

  private List<Contact> handleContacts(final Multimap<String, String> multiMap) {
    final List<Contact> contactList = new ArrayList<>();
    final List<String> contactEmails = new ArrayList<>();
    final List<String> contactNames = new ArrayList<>();
    final List<String> contactAffiliations = new ArrayList<>();
    final List<String> contactRoles = new ArrayList<>();
    final List<String> contactUrls = new ArrayList<>();

    multiMap
        .entries()
        .forEach(
            entry -> {
              final String entryKey = entry.getKey();
              final String entryValue = entry.getValue();

              if (entryKey.startsWith("comment[submission_contact:")) {
                if (entryKey.contains("email")) {
                  contactEmails.add(entryValue);
                }

                if (entryKey.contains("name")) {
                  contactNames.add(entryValue);
                }

                if (entryKey.contains("affiliation")) {
                  contactAffiliations.add(entryValue);
                }

                if (entryKey.contains("role")) {
                  contactRoles.add(entryValue);
                }

                if (entryKey.contains("url")) {
                  contactUrls.add(entryValue);
                }
              }
            });

    /*Contact email is mandatory for the contact information to be built*/
    for (int iter = 0; iter < contactEmails.size(); iter++) {
      final Contact.Builder contactBuilder = new Contact.Builder();

      contactBuilder.email(contactEmails.get(iter));

      contactBuilder.name(iter >= contactNames.size() ? null : contactNames.get(iter));
      contactBuilder.affiliation(
          iter >= contactAffiliations.size() ? null : contactAffiliations.get(iter));
      contactBuilder.role(iter >= contactRoles.size() ? null : contactRoles.get(iter));
      contactBuilder.url(iter >= contactUrls.size() ? null : contactUrls.get(iter));

      if (contactBuilder.isNotEmpty()) {
        contactList.add(contactBuilder.build());
      }
    }

    return contactList;
  }

  private List<Publication> handlePublications(final Multimap<String, String> multiMap) {
    final List<String> publicationDois = handlePublicationDois(multiMap);
    final List<String> publicationPubMedIds = handlePublicationPubMedIds(multiMap);
    final List<Publication> publicationList = new ArrayList<>();

    final int pubMedSize = publicationPubMedIds.size();
    final int doiSize = publicationDois.size();

    if (pubMedSize >= doiSize) {
      for (int iter = 0; iter < pubMedSize; iter++) {
        publicationList.add(
            buildPublication(publicationDois, publicationPubMedIds, iter, pubMedSize, doiSize));
      }
    } else {
      for (int iter = 0; iter < doiSize; iter++) {
        publicationList.add(
            buildPublication(publicationDois, publicationPubMedIds, iter, pubMedSize, doiSize));
      }
    }

    return publicationList;
  }

  private Publication buildPublication(
      final List<String> publicationDois,
      final List<String> publicationPubMedIds,
      final int iter,
      final int pubMedSize,
      final int doiSize) {
    final Publication.Builder publicationBuilder = new Publication.Builder();

    final String pubMedId = iter >= pubMedSize ? null : publicationPubMedIds.get(iter);
    final String doi = iter >= doiSize ? null : publicationDois.get(iter);

    publicationBuilder.pubmed_id(pubMedId);
    publicationBuilder.doi(doi);

    return publicationBuilder.build();
  }

  private List<String> handlePublicationPubMedIds(final Multimap<String, String> multiMap) {
    final List<String> pubMedIds = new ArrayList<>();

    multiMap
        .entries()
        .forEach(
            entry -> {
              final String entryKey = entry.getKey();
              final String entryValue = entry.getValue();

              if (entryKey.startsWith("comment[publication:")) {
                if (entryKey.contains("pubmed_id")) {
                  pubMedIds.add((entryValue != null && !entryValue.isEmpty()) ? entryValue : "");
                }
              }
            });

    return pubMedIds.stream().filter(Objects::nonNull).collect(Collectors.toList());
  }

  private List<String> handlePublicationDois(final Multimap<String, String> multiMap) {
    final List<String> dois = new ArrayList<>();

    multiMap
        .entries()
        .forEach(
            entry -> {
              final String entryKey = entry.getKey();
              final String entryValue = entry.getValue();

              if (entryKey.startsWith("comment[publication:")) {
                if (entryKey.contains("doi")) {
                  dois.add((entryValue != null && !entryValue.isEmpty()) ? entryValue : "");
                }
              }
            });

    return dois.stream().filter(Objects::nonNull).collect(Collectors.toList());
  }

  public String getSampleAccession(final Multimap<String, String> multiMap) {
    return multiMap.get("sample identifier").stream().findFirst().orElse(null);
  }

  private List<ExternalReference> handleExternalReferences(
      final Multimap<String, String> multiMap) {
    final List<ExternalReference> externalReferenceList = new ArrayList<>();

    multiMap
        .entries()
        .forEach(
            entry -> {
              final String entryKey = entry.getKey();
              final String entryValue = entry.getValue();

              log.trace(entryKey + " " + entryValue);

              if (entryKey.startsWith("comment") && entryKey.contains("external db ref")) {
                if (entryValue != null && entryValue.length() > 1) {
                  externalReferenceList.add(ExternalReference.build(entryValue));
                }
              }
            });

    return externalReferenceList;
  }

  public List<Relationship> createRelationships(
      final Sample sample,
      final Map<String, String> sampleNameToAccessionMap,
      final Multimap<String, String> relationshipMap,
      final ValidationResult validationResult) {
    return relationshipMap.entries().stream()
        .map(entry -> getRelationship(sample, sampleNameToAccessionMap, entry, validationResult))
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }

  private Relationship getRelationship(
      final Sample sample,
      final Map<String, String> sampleNameToAccessionMap,
      final Map.Entry<String, String> entry,
      final ValidationResult validationResult) {
    if (sample != null && sample.getAccession() != null) {
      try {
        final String relationshipTarget = getRelationshipTarget(sampleNameToAccessionMap, entry);

        if (relationshipTarget != null) {
          return Relationship.build(
              sample.getAccession(), entry.getKey().trim(), relationshipTarget);
        } else {
          validationResult.addValidationMessage(
              new ValidationResult.ValidationMessage(
                  sample.getAccession(),
                  "Failed to add all relationships for " + sample.getAccession(),
                  true));

          return null;
        }
      } catch (final Exception e) {
        log.info("Failed to add relationship");
        validationResult.addValidationMessage(
            new ValidationResult.ValidationMessage(
                sample.getAccession(),
                "Failed to add all relationships for " + sample.getAccession(),
                true));

        return null;
      }
    }

    return null;
  }

  private String getRelationshipTarget(
      final Map<String, String> sampleNameToAccessionMap, final Map.Entry<String, String> entry) {
    final String relationshipTarget = entry.getValue().trim();

    if (relationshipTarget.startsWith("SAM")) {
      return relationshipTarget;
    } else {
      return sampleNameToAccessionMap.get(relationshipTarget);
    }
  }

  public Multimap<String, String> parseRelationships(final Multimap<String, String> multiMap) {
    return multiMap.entries().stream()
        .filter(
            e -> {
              final String entryKey = e.getKey();

              return entryKey.startsWith("comment") && entryKey.contains("bsd_relationship");
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
  }

  private List<Characteristics> handleCharacteristics(final Multimap<String, String> multiMap) {
    final List<Characteristics> characteristicsList = new ArrayList<>();

    multiMap
        .entries()
        .forEach(
            entry -> {
              final Characteristics characteristics = new Characteristics();

              if (entry.getKey().startsWith("Characteristics")) {
                characteristics.setName(entry.getKey().trim());
                characteristics.setValue(entry.getValue());

                characteristicsList.add(characteristics);
              }
            });

    final List<String> termRefList =
        multiMap.entries().stream()
            .map(
                entry -> {
                  if (entry.getKey().startsWith("term accession number")) {
                    final String value = entry.getValue();

                    return value != null ? value.trim() : null;
                  } else {
                    return null;
                  }
                })
            .filter(Objects::nonNull)
            .toList();

    final List<String> unitList =
        multiMap.entries().stream()
            .map(
                entry -> {
                  if (entry.getKey().startsWith("unit")) {
                    final String value = entry.getValue();

                    log.info(
                        "Unit is " + value + " for " + entry.getKey() + " and " + entry.getValue());

                    return value != null ? value.trim() : null;
                  } else {
                    return null;
                  }
                })
            .filter(Objects::nonNull)
            .toList(); // handle units

    final AtomicInteger i = new AtomicInteger(0);

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

  public static String getSampleName(final Multimap<String, String> multiMap) {
    return multiMap.get("sample name").stream().findFirst().orElse(null);
  }

  private static String getReleaseDate(final Multimap<String, String> multiMap) {
    return multiMap.get("release date").stream().findFirst().orElse(null);
  }

  public Sample addChecklistAttributeAndBuildSample(final String checklist, Sample sample) {
    final Set<Attribute> attributeSet = sample.getAttributes();
    final Attribute attribute = new Attribute.Builder("checklist", checklist).build();

    attributeSet.add(attribute);
    sample = Sample.Builder.fromSample(sample).withAttributes(attributeSet).build();

    return sample;
  }

  private boolean isValidSample(
      final String sampleName,
      final String sampleReleaseDate,
      final ValidationResult validationResult) {
    boolean isValidSample = true;

    if (sampleName == null || sampleName.isEmpty()) {
      validationResult.addValidationMessage(
          new ValidationResult.ValidationMessage(
              "MESSAGE#1",
              "All samples in the file must have a sample name, some samples are missing sample name and hence are not created",
              true));
      isValidSample = false;
    }

    if (sampleReleaseDate == null || sampleReleaseDate.isEmpty()) {
      validationResult.addValidationMessage(
          new ValidationResult.ValidationMessage(
              "MESSAGE#2",
              "All samples in the file must have a release date "
                  + sampleName
                  + " doesn't have a release date and is not created",
              true));
      isValidSample = false;
    }

    return isValidSample;
  }

  private <T> List<T> getPrintableListFromCsvRow(final Iterator<T> iterator) {
    final Iterable<T> iterable = () -> iterator;

    return StreamSupport.stream(iterable.spliterator(), false).collect(Collectors.toList());
  }

  public File writeToFile(
      final File fileToBeUploaded,
      final List<String> headers,
      final List<Sample> samples,
      final ValidationResult validationResult) {
    try {
      log.info("Writing to file");
      final Path temp = Files.createTempFile("upload_result", ".tsv");
      final boolean headerHasIdentifier =
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
        for (final CSVRecord row : records) {
          if (headerHasIdentifier) {
            csvPrinter.printRecord(row);
          } else {
            csvPrinter.printRecord(
                addAccessionToSamplesForPrint(getPrintableListFromCsvRow(row.iterator()), samples));
          }
        }

        out.println("\n\n");
        out.println("********RECEIPT START********");
        out.println(
            validationResult.getValidationMessagesList().stream()
                .map(
                    validationMessage ->
                        validationMessage.getMessageKey()
                            + " : "
                            + validationMessage.getMessageValue())
                .collect(Collectors.joining("\n")));
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
      final List<String> listFromIterator, final List<Sample> samples) {
    final String sampleName = listFromIterator.get(1);
    final Optional<Sample> sampleOptional =
        samples.stream().filter(sample -> sample.getName().equals(sampleName)).findFirst();

    sampleOptional.ifPresent(sample -> listFromIterator.add(sample.getAccession()));

    return listFromIterator;
  }

  public CSVParser buildParser(final BufferedReader reader) throws IOException {
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

  public File writeQueueMessageToFile(final String submissionId) throws IOException {
    final Path temp = Files.createTempFile("queue_result", ".txt");

    try (final BufferedWriter writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
      writer.write(
          "Your submission has been queued and your submission id is "
              + submissionId
              + ". Please use the View Submissions tab and use your submission ID to get the submission result.");
    }

    return temp.toFile();
  }

  public void validateHeaderPositions(
      final List<String> headers, final ValidationResult validationResult) {
    if (!headers.isEmpty()) {
      if ((!headers.get(0).equalsIgnoreCase("Source Name")
          && (!headers.get(1).equalsIgnoreCase("Sample Name"))
          && (!headers.get(2).equalsIgnoreCase("Release Date")))) {
        validationResult.addValidationMessage(
            new ValidationResult.ValidationMessage(
                "GENERAL_VALIDATION_MESSAGE",
                "ISA tab file must have Source Name as first column, followed by Sample Name and Release Date.",
                true));

        throw new GlobalExceptions.UploadInvalidException(
            validationResult.getValidationMessagesList().stream()
                .map(
                    validationMessage ->
                        validationMessage.getMessageKey()
                            + ":"
                            + validationMessage.getMessageValue())
                .collect(Collectors.joining("\n")));
      }
    }
  }

  public static String formatDateString(LocalDateTime dateTime) {
    return dateTime.format(DATE_TIME_FORMATTER);
  }
}
