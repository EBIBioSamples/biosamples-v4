package uk.ac.ebi.biosamples.utils.upload;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.biosamples.model.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.google.common.collect.Multimaps.toMultimap;

public class FileUploadUtils {
    private Logger log = LoggerFactory.getLogger(getClass());

    public List<Multimap<String, String>> getCSVDataInMap(final CSVParser csvParser) throws IOException {
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
                                        String record = csvRecord.get(i.get());
                                        listMultiMap.put(header, record);
                                        i.getAndIncrement();
                                    });

                            csvDataMap.add(listMultiMap);
                        });

        return csvDataMap;
    }

    public Sample buildSample(final String sampleName, final String accession, final List<Characteristics> characteristicsList, final List<ExternalReference> externalReferenceList,
                              final List<Contact> contactsList) {
        return new Sample.Builder(sampleName.trim())
                .withAccession(accession)
                .withAttributes(
                        characteristicsList.stream()
                                .map(
                                        characteristics -> {
                                            final String name = characteristics.getName();
                                            final String trimmedCharacteristicsName = name.substring(name.indexOf('[') + 1, name.indexOf(']'));
                                            final String characteristicsValue = characteristics.getValue();

                                            if (isValidCharacteristics(name, trimmedCharacteristicsName, characteristicsValue)) {

                                                return new Attribute.Builder(
                                                        trimmedCharacteristicsName,
                                                        characteristicsValue)
                                                        .withTag("attribute")
                                                        .withUnit(characteristics.getUnit())
                                                        .withIri(characteristics.getIri())
                                                        .build();
                                            } else {
                                                return null;
                                            }
                                        }).filter(Objects::nonNull)
                                .collect(Collectors.toList()))
                .withExternalReferences(externalReferenceList)
                .withContacts(contactsList)
                .build();
    }

    private boolean isValidCharacteristics(final String name, final String trimmedCharacteristicsName, final String characteristicsValue) {
        return (name != null && !trimmedCharacteristicsName.isEmpty()) && (characteristicsValue != null && !characteristicsValue.isEmpty());
    }

    public List<Contact> handleContacts(final Multimap<String, String> multiMap) {
        final List<Contact> contactList = new ArrayList<>();

        multiMap
                .entries()
                .forEach(
                        entry -> {
                            final String entryKey = entry.getKey();
                            final String entryValue = entry.getValue();

                            log.trace(entryKey + " " + entryValue);

                            if (entryKey.startsWith("Comment") && entryKey.contains("submission_contact")) {
                                contactList.add(new Contact.Builder().email(entry.getValue()).build());
                            }
                        });

        return contactList;
    }

    public String getSampleAccession(final Multimap<String, String> multiMap) {
        Optional<String> sampleAccession = multiMap.get("Sample Identifier").stream().findFirst();

        return sampleAccession.orElse(null);
    }

    public List<ExternalReference> handleExternalReferences(final Multimap<String, String> multiMap) {
        List<ExternalReference> externalReferenceList = new ArrayList<>();

        multiMap
                .entries()
                .forEach(
                        entry -> {
                            final String entryKey = entry.getKey();
                            final String entryValue = entry.getValue();

                            log.trace(entryKey + " " + entryValue);

                            if (entryKey.startsWith("Comment") && entryKey.contains("external DB REF")) {
                                externalReferenceList.add(ExternalReference.build(entry.getValue()));
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

    public Relationship getRelationship(
            final Sample sample,
            final Map<String, String> sampleNameToAccessionMap,
            final Map.Entry<String, String> entry,
            final ValidationResult validationResult) {
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

    public String getRelationshipTarget(
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
    }

    public List<Characteristics> handleCharacteristics(final Multimap<String, String> multiMap) {
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

    public String getSampleName(final Multimap<String, String> multiMap) {
        final Optional<String> sampleName = multiMap.get("Sample Name").stream().findFirst();

        return sampleName.orElse(null);
    }

    public String getReleaseDate(final Multimap<String, String> multiMap) {
        final Optional<String> sampleReleaseDate = multiMap.get("Release Date").stream().findFirst();

        return sampleReleaseDate.orElse(null);
    }

    public Sample addChecklistAttributeAndBuildSample(final String checklist, Sample sample) {
        final Set<Attribute> attributeSet = sample.getAttributes();
        final Attribute attribute =
                new Attribute.Builder("checklist", checklist.substring(0, checklist.indexOf('(')))
                        .build();

        attributeSet.add(attribute);
        sample = Sample.Builder.fromSample(sample).withAttributes(attributeSet).build();

        return sample;
    }

    public boolean isBasicValidationFailure(final String sampleName, final String sampleReleaseDate, final ValidationResult validationResult) {
        boolean basicValidationFailure = false;

        if (sampleName == null || sampleName.isEmpty()) {
            validationResult.addValidationMessage(
                    "All samples in the file must have a sample name, some samples are missing sample name and hence are not created");
            basicValidationFailure = true;
        }

        if (sampleReleaseDate == null || sampleReleaseDate.isEmpty()) {
            validationResult.addValidationMessage(
                    "All samples in the file must have a release date "
                            + sampleName
                            + " doesn't have a release date and is not created");
            basicValidationFailure = true;
        }

        return !basicValidationFailure;
    }

    private <T> List<T> getPrintableListFromCsvRow(final Iterator<T> iterator) {
        final Iterable<T> iterable = () -> iterator;

        return StreamSupport.stream(iterable.spliterator(), false).collect(Collectors.toList());
    }

    public File writeToFile(final File fileToBeUploaded, final List<String> headers, final List<Sample> samples, ValidationResult validationResult) {
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
}
