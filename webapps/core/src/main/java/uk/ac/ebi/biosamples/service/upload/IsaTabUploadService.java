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
import uk.ac.ebi.biosamples.model.upload.Characteristics;
import uk.ac.ebi.biosamples.model.upload.validation.Messages;
import uk.ac.ebi.biosamples.model.upload.validation.ValidationResult;
import uk.ac.ebi.biosamples.service.SampleService;
import uk.ac.ebi.biosamples.service.certification.CertifyService;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class IsaTabUploadService {
    private Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    ValidationResult validationResult;

    @Autowired
    SampleService sampleService;

    @Autowired
    CertifyService certifyService;

    @Autowired
    Messages messages;

    public File upload(MultipartFile file, String aapDomain, String certificate, String webinId) throws IOException {
        validationResult.clear();
        messages.clear();

        final List<Multimap<String, String>> csvDataMap = new ArrayList<>();
        Path temp = Files.createTempFile("upload", ".tsv");

        File fileToBeUploaded = temp.toFile();
        file.transferTo(fileToBeUploaded);

        log.info("Input file name " + fileToBeUploaded.getName());

        FileReader fr = new FileReader(fileToBeUploaded);
        BufferedReader reader = new BufferedReader(fr);

        final CSVParser csvParser = buildParser(reader);
        final List<String> headers = csvParser.getHeaderNames();

        csvParser.getRecords().forEach(csvRecord -> {
            final AtomicInteger i = new AtomicInteger(0);
            final Multimap<String, String> listMultiMap = LinkedListMultimap.create();

            headers.forEach(header -> {
                String record = csvRecord.get(i.get());
                listMultiMap.put(header, record);
                i.getAndIncrement();
            });

            csvDataMap.add(listMultiMap);
        });

        log.info("CSV data size: " + csvDataMap.size());

        try {
            final List<Sample> samples = buildSamples(csvDataMap, aapDomain, webinId, certificate);

            log.info("Number of samples persisted: " + samples.size());
            writeToFile(fileToBeUploaded, headers);

            return fileToBeUploaded;
        } catch (Exception e) {
            throw new UploadInvalidException(validationResult.getValidationMessagesList().stream().collect(Collectors.joining("\n")));
        }
    }

    private void writeToFile(File fileToBeUploaded, List<String> headers) throws IOException {
        log.info("Writing to file");

        final List<String> outputFileHeaders = new ArrayList<>(headers);
        //outputFileHeaders.add("Sample Identifier");
        final Reader in = new FileReader(fileToBeUploaded);
        final String[] headerParsed = headers.toArray(new String[headers.size()]);
        final Iterable<CSVRecord> records = CSVFormat.TDF
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
        final FileWriter fileWriter = new FileWriter(fileToBeUploaded);

        try (final CSVPrinter csvPrinter = new CSVPrinter(fileWriter, CSVFormat.TDF.withHeader(headerParsed))) {
            try {
                for (CSVRecord row : records) {
                    csvPrinter.printRecord(row);
                }
            } finally {
                fileWriter.flush();
                fileWriter.close();
            }
        }
    }

    private List<Sample> buildSamples(List<Multimap<String, String>> csvDataMap, String aapDomain, String webinId, String certificate) {
        final Map<String, String> sampleNameToAccessionMap = new LinkedHashMap<>();
        final Map<Sample, Multimap<String, String>> sampleToMappedSample = new LinkedHashMap<>();

        csvDataMap.forEach(csvRecordMap -> {
            Sample sample = null;

            try {
                sample = buildSample(csvRecordMap, aapDomain, webinId, certificate);
            } catch (Exception e) {
                validationResult.addValidationMessage("Failed to create all samples in the file " + e.getMessage());
            }
            sampleNameToAccessionMap.put(sample.getName(), sample.getAccession());
            sampleToMappedSample.put(sample, csvRecordMap);
        });

        return addRelationshipsAndThenBuildSamples(sampleNameToAccessionMap, sampleToMappedSample, aapDomain, webinId);
    }

    private boolean isWebinIdUsedToAuthenticate(String webinId) {
        return webinId != null && webinId.toUpperCase().startsWith("WEBIN");
    }

    private List<Sample> addRelationshipsAndThenBuildSamples(Map<String, String> sampleNameToAccessionMap, Map<Sample, Multimap<String, String>> sampleToMappedSample,
                                                             String aapDomain, String webinId) {
        return sampleToMappedSample
                .entrySet()
                .stream()
                .map(sampleMultimapEntry -> addRelationshipAndThenBuildSample(sampleNameToAccessionMap, sampleMultimapEntry, aapDomain, webinId))
                .collect(Collectors.toList());
    }

    private Sample addRelationshipAndThenBuildSample(Map<String, String> sampleNameToAccessionMap, Map.Entry<Sample, Multimap<String, String>> sampleMultimapEntry,
                                                     String aapDomain, String webinId) {
        final String authProvider = isWebinIdUsedToAuthenticate(webinId) ? "WEBIN" : "AAP";
        final Map<String, String> relationshipMap = parseRelationships(sampleMultimapEntry.getValue());
        Sample sample = sampleMultimapEntry.getKey();
        final List<Relationship> relationships = createRelationships(sample, sampleNameToAccessionMap, relationshipMap);

        relationships.forEach(relationship -> log.info(relationship.toString()));

        sample = Sample.Builder.fromSample(sample).withRelationships(relationships).build();
        sample = sampleService.store(sample, false, true, authProvider);

        return sample;
    }

    private Sample buildSample(Multimap<String, String> multiMap, String aapDomain, String webinId, String certificate) throws JsonProcessingException, UploadInvalidException {
        final String sampleName = getSampleName(multiMap);
        final String accession = getSampleAccession(multiMap);
        final List<Characteristics> characteristicsList = handleCharacteristics(multiMap);
        final List<ExternalReference> externalReferenceList = handleExternalReferences(multiMap);
        final String authProvider = isWebinIdUsedToAuthenticate(webinId) ? "WEBIN" : "AAP";

        if (sampleName == null || sampleName.isEmpty()) {
            validationResult.addValidationMessage("All samples in the file must have a sample name, some samples are missing sample name and hence are not created");
        }

        externalReferenceList.forEach(externalReference -> log.info(externalReference.toString()));

        Sample sample = new Sample.Builder(sampleName.trim())
                .withAccession(accession)
                .withAttributes(characteristicsList.stream()
                        .map(characteristics -> {
                            final String name = characteristics.getName();

                            return new Attribute.Builder
                                    (name.substring(name.indexOf('[') + 1, name.indexOf(']')),
                                            characteristics.getValue())
                                    .withTag("attribute")
                                    .withUnit(characteristics.getUnit())
                                    .withIri(characteristics.getIri())
                                    .build();
                        })
                        .collect(Collectors.toList()))
                .withExternalReferences(externalReferenceList)
                .build();

        if (sampleService.isWebinAuthorization(authProvider)) {
            sample =
                    Sample.Builder.fromSample(sample).withWebinSubmissionAccountId(webinId).build();
        } else {
            sample = Sample.Builder.fromSample(sample).withDomain(aapDomain).build();
        }

        if (certify(sample, certificate)) {
            sample = sampleService.store(sample, false, true, authProvider);
        }
        
        log.info("Sample " + sample.getName() + " created with accession " + sample.getAccession());

        return sample;
    }

    private String getSampleAccession(Multimap<String, String> multiMap) {
        Optional<String> sampleAccession = multiMap.get("Sample Identifier").stream().findFirst();

        return sampleAccession.orElse(null);
    }

    private List<ExternalReference> handleExternalReferences(Multimap<String, String> multiMap) {
        List<ExternalReference> externalReferenceList = new ArrayList<>();

        multiMap.entries().forEach(entry -> {
            final String entryKey = entry.getKey();
            final String entryValue = entry.getValue();

            log.info(entryKey + " " + entryValue);

            if (entryKey.startsWith("Comment") && entryKey.contains("external DB REF")) {
                externalReferenceList.add(ExternalReference.build(entry.getValue()));
            }
        });

        return externalReferenceList;
    }

    private List<Relationship> createRelationships(Sample sample, Map<String, String> sampleNameToAccessionMap, Map<String, String> relationshipMap) {
        return relationshipMap
                .entrySet()
                .stream()
                .map(entry ->
                        getRelationship(sample, sampleNameToAccessionMap, entry))
                .collect(Collectors.toList());
    }

    private Relationship getRelationship(Sample sample, Map<String, String> sampleNameToAccessionMap, Map.Entry<String, String> entry) {
        try {
            return Relationship.build(sample.getAccession(), entry.getKey().trim(), sampleNameToAccessionMap.get(entry.getValue().trim()));
        } catch (Exception e) {
            validationResult.addValidationMessage("Failed to add all relationships for " + sample.getAccession());
            return null;
        }
    }

    private boolean certify(Sample sample, String certificate) throws JsonProcessingException {
        List<Certificate> certificates =
                certifyService.certify(objectMapper.writeValueAsString(sample), false, certificate);

        return certificates.size() != 0;
    }

    private Map<String, String> parseRelationships(
            Multimap<String, String> multiMap) {
        return
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
                                        LinkedHashMap::new));
    }

    private List<Characteristics> handleCharacteristics(Multimap<String, String> multiMap) {
        final List<Characteristics> characteristicsList = new ArrayList<>();

        multiMap.entries().forEach(entry -> {
            final Characteristics characteristics = new Characteristics();
            if (entry.getKey().startsWith("Characteristics")) {
                characteristics.setName(entry.getKey().trim());
                final String value = entry.getValue();

                characteristics.setValue(value);

                characteristicsList.add(characteristics);
            }
        });

        List<String> termRefList = multiMap.entries().stream().map(entry -> {
            if (entry.getKey().startsWith("Term Accession Number")) {
                final String value = entry.getValue();

                return value != null ? value.trim() : null;
            } else return null;
        }).filter(Objects::nonNull).collect(Collectors.toList());

        List<String> unitList = multiMap.entries().stream().map(entry -> {
            if (entry.getKey().startsWith("Unit")) {
                final String value = entry.getValue();

                return value != null ? value.trim() : null;
            } else return null;
        }).filter(Objects::nonNull).collect(Collectors.toList()); // handle units

        AtomicInteger i = new AtomicInteger(0);

        characteristicsList.forEach(characteristics -> {
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

    private CSVParser buildParser(BufferedReader reader) throws IOException {
        return new CSVParser(reader, CSVFormat.TDF
                .withAllowDuplicateHeaderNames()
                .withFirstRecordAsHeader()
                .withIgnoreEmptyLines()
                .withIgnoreHeaderCase()
                .withAllowMissingColumnNames()
                .withIgnoreSurroundingSpaces()
                .withTrim());
    }
}
