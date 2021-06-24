package uk.ac.ebi.biosamples.utils.upload;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.biosamples.model.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.google.common.collect.Multimaps.toMultimap;

public class FileUploadIsaTabUtils {
    private Logger log = LoggerFactory.getLogger(getClass());

    public List<Multimap<String, String>> getCSVDataInMap(CSVParser csvParser) throws IOException {
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

    public Sample buildSample(String sampleName, String accession, List<Characteristics> characteristicsList, List<ExternalReference> externalReferenceList, List<Contact> contactsList) {
        return new Sample.Builder(sampleName.trim())
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
    }

    public List<Contact> handleContacts(Multimap<String, String> multiMap) {
        List<Contact> contactList = new ArrayList<>();

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

    public String getSampleAccession(Multimap<String, String> multiMap) {
        Optional<String> sampleAccession = multiMap.get("Sample Identifier").stream().findFirst();

        return sampleAccession.orElse(null);
    }

    public List<ExternalReference> handleExternalReferences(Multimap<String, String> multiMap) {
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
            Sample sample,
            Map<String, String> sampleNameToAccessionMap,
            Multimap<String, String> relationshipMap,
            ValidationResult validationResult) {
        return relationshipMap.entries().stream()
                .map(entry -> getRelationship(sample, sampleNameToAccessionMap, entry, validationResult))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public Relationship getRelationship(
            Sample sample,
            Map<String, String> sampleNameToAccessionMap,
            Map.Entry<String, String> entry,
            ValidationResult validationResult) {
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
            Map<String, String> sampleNameToAccessionMap, Map.Entry<String, String> entry) {
        final String relationshipTarget = entry.getValue().trim();

        if (relationshipTarget.startsWith("SAM")) {
            return relationshipTarget;
        } else {
            return sampleNameToAccessionMap.get(relationshipTarget);
        }
    }

    public Multimap<String, String> parseRelationships(Multimap<String, String> multiMap) {
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

    public List<Characteristics> handleCharacteristics(Multimap<String, String> multiMap) {
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

    public String getSampleName(Multimap<String, String> multiMap) {
        Optional<String> sampleName = multiMap.get("Sample Name").stream().findFirst();

        return sampleName.orElse(null);
    }

    public String getReleaseDate(Multimap<String, String> multiMap) {
        Optional<String> sampleReleaseDate = multiMap.get("Release Date").stream().findFirst();

        return sampleReleaseDate.orElse(null);
    }

    public CSVParser buildParser(BufferedReader reader) throws IOException {
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
