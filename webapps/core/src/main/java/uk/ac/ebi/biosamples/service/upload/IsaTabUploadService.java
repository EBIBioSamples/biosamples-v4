package uk.ac.ebi.biosamples.service.upload;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.Certificate;
import uk.ac.ebi.biosamples.model.Relationship;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.upload.Characteristics;
import uk.ac.ebi.biosamples.model.upload.validation.Messages;
import uk.ac.ebi.biosamples.model.upload.validation.ValidationResult;
import uk.ac.ebi.biosamples.service.SampleService;
import uk.ac.ebi.biosamples.service.certification.CertifyService;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
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

    public void upload(MultipartFile file, String aapDomain, String certificate, String webinId) throws IOException {
        final List<Multimap<String, String>> csvDataMap = new ArrayList<>();
        Path temp = Files.createTempFile("upload", ".tsv");

        File fileToBeUploaded = temp.toFile();
        file.transferTo(fileToBeUploaded);

        FileReader fr = new FileReader(fileToBeUploaded);
        BufferedReader reader = new BufferedReader(fr);

        final CSVParser csvParser = buildParser(reader);
        final List<String> headers = csvParser.getHeaderNames();

        csvParser.getRecords().forEach(csvRecord -> {
            final AtomicInteger i = new AtomicInteger(0);
            final Multimap<String, String> listMultiMap = ArrayListMultimap.create();

            headers.forEach(header -> {
                String record = csvRecord.get(i.get());
                listMultiMap.put(header, record);
                i.getAndIncrement();
            });

            csvDataMap.add(listMultiMap);
        });

        log.info("CSV data size: " + csvDataMap.size());

        List<Sample> samples = buildSamples(csvDataMap, aapDomain, webinId, certificate);

        log.info("Number of samples persisted: " + samples.size());
    }

    private List<Sample> buildSamples(List<Multimap<String, String>> csvDataMap, String aapDomain, String webinId, String certificate) {
        final Map<String, String> sampleNameToAccessionMap = new LinkedHashMap<>();
        final Map<Sample, Multimap<String, String>> sampleToMappedSample = new LinkedHashMap<>();

        csvDataMap.forEach(csvRecordMap -> {
            Sample sample = null;

            try {
                sample = buildSample(csvRecordMap, aapDomain, webinId, certificate);
            } catch (JsonProcessingException e) {
                e.printStackTrace(); // throw from here
            }
            sampleNameToAccessionMap.put(sample.getName(), sample.getAccession());
            sampleToMappedSample.put(sample, csvRecordMap);
        });

        return addRelationshipsAndThenBuildSamples(sampleNameToAccessionMap, sampleToMappedSample);
    }

    private boolean isWebinIdUsedToAuthenticate(String webinId) {
        return webinId != null && webinId.toUpperCase().startsWith("WEBIN");
    }

    private List<Sample> addRelationshipsAndThenBuildSamples(Map<String, String> sampleNameToAccessionMap, Map<Sample, Multimap<String, String>> sampleToMappedSample) {
        return sampleToMappedSample
                .entrySet()
                .stream()
                .map(sampleMultimapEntry -> addRelationshipAndThenBuildSample(sampleNameToAccessionMap, sampleMultimapEntry))
                .collect(Collectors.toList());
    }

    private Sample addRelationshipAndThenBuildSample(Map<String, String> sampleNameToAccessionMap, Map.Entry<Sample, Multimap<String, String>> sampleMultimapEntry) {
        final Map<String, String> relationshipMap = parseRelationships(sampleMultimapEntry.getValue());
        Sample sample = sampleMultimapEntry.getKey();
        final List<Relationship> relationships = createRelationships(sample, sampleNameToAccessionMap, relationshipMap);

        relationships.forEach(relationship -> log.info(relationship.toString()));

        sample = Sample.Builder.fromSample(sample).withRelationships(relationships).build();
        sample = sampleService.store(sample, false, true, "AAP");

        return sample;
    }

    private Sample buildSample(Multimap<String, String> multiMap, String aapDomain, String webinId, String certificate) throws JsonProcessingException {
        final String sampleName = getSampleName(multiMap);
        final List<Characteristics> characteristicsList = handleCharacteristics(multiMap);
        final String authProvider = isWebinIdUsedToAuthenticate(webinId) ? "WEBIN" : "AAP";

        Sample sample = new Sample.Builder(sampleName.trim())
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
                .build();

        if (sampleService.isWebinAuthorization(authProvider)) {
            sample =
                    Sample.Builder.fromSample(sample).withWebinSubmissionAccountId(webinId).build();
        } else {
            sample = Sample.Builder.fromSample(sample).withDomain(aapDomain).build();
        }

        if (certify(sample, certificate)) {
            sample = sampleService.store(sample, false, true, "AAP");
        }
        log.info("Sample " + sample.getName() + " created with accession " + sample.getAccession());

        return sample;
    }

    private List<Relationship> createRelationships(Sample sample, Map<String, String> sampleNameToAccessionMap, Map<String, String> relationshipMap) {
        return relationshipMap
                .entrySet()
                .stream()
                .map(entry ->
                        Relationship.build(sample.getAccession(), entry.getKey().trim(), sampleNameToAccessionMap.get(entry.getValue().trim())))
                .collect(Collectors.toList());
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

    private static String getSampleName(Multimap<String, String> multiMap) {
        Optional<String> sampleName = multiMap.get("Sample Name").stream().findFirst();

        return sampleName.orElse(null);

    }

    private static CSVParser buildParser(BufferedReader reader) throws IOException {
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
