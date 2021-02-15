package uk.ac.ebi.biosamples.service.upload;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvParser;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.Certificate;
import uk.ac.ebi.biosamples.model.Relationship;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.upload.validation.ValidationResult;
import uk.ac.ebi.biosamples.service.SampleService;
import uk.ac.ebi.biosamples.service.certification.CertifyService;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Service
public class FileUploadService {
    private Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    ValidationResult validationResult;

    @Autowired
    SampleService sampleService;

    @Autowired
    CertifyService certifyService;

    public void upload(MultipartFile file, String aapDomain, String certificate) throws IOException {
        Path temp = Files.createTempFile("test", ".tmp");

        File fileToBeUploaded = temp.toFile();
        file.transferTo(fileToBeUploaded);

        validationResult.clear();

        List<Map<?, ?>> data = readObjectsFromCsv(fileToBeUploaded);
        String json = writeAsJson(data);

        log.info("JSON is" + json);

        List<Map<String, Object>> jsonListOfMappedSamples = objectMapper.readValue(json,
                new TypeReference<List<Map<String, Object>>>() {
                });

        createSamplesFromMappedData(jsonListOfMappedSamples, aapDomain, certificate);
    }

    private void createSamplesFromMappedData(List<Map<String, Object>> jsonListOfMappedSamples, String aapDomain, String certificate) {
        final Map<Sample, Map<String, Object>> sampleToMappedSample = new LinkedHashMap<>();

        jsonListOfMappedSamples.forEach(mappedSample -> {
            try {
                Sample sample = validateAndBuildSample(mappedSample);
                sample = Sample.Builder.fromSample(sample).withDomain(aapDomain).build();

                if (certify(sample, certificate)) {
                    sample = sampleService.store(sample, true);
                    sampleToMappedSample.put(sample, mappedSample);
                } else {
                    validationResult.addValidationMessage(sample.getName() + " failed validation against " + certificate);
                }
            } catch (Exception e) {
                log.info(getValidationMessages("\n"));
            }
        });

        List<Sample> samples = sampleToMappedSample.entrySet()
                .stream()
                .map(sampleMapEntry -> parseRelationships(sampleMapEntry.getValue(), sampleMapEntry.getKey()))
                .collect(Collectors.toList());

        samples = samples.stream().map(sample -> sampleService.store(sample, true)).collect(Collectors.toList());

        log.info(String.valueOf(samples.size()));

        samples.forEach(sample -> log.info(sample.toString()));
    }

    private boolean certify(Sample sample, String certificate) throws JsonProcessingException {
        log.info("Certificate is " + certificate);

        List<Certificate> certificates =
                certifyService.certify(objectMapper.writeValueAsString(sample), false, certificate);

        if (certificates.size() == 0) {
            return true;
        }

        return false;
    }

    private Sample validateAndBuildSample(Map<String, Object> mappedSample) {
        AtomicReference<Sample> sample = new AtomicReference<>();
        AtomicReference<String> name = new AtomicReference<>();
        AtomicReference<String> release = new AtomicReference<>();

        mappedSample.forEach((key, value) -> {
            if (key.equalsIgnoreCase("Sample Name")) {
                if (value != null) {
                    name.set(value.toString());
                }
            }

            if (key.equalsIgnoreCase("Release")) {
                if (value != null) {
                    release.set(value.toString());
                }
            }
        });

        if (name.get().isEmpty()) {
            validationResult.addValidationMessage(" Sample name not present ");
            throw new RuntimeException(getValidationMessages(","));
        }

        if (release.get().isEmpty()) {
            validationResult.addValidationMessage(" Sample release date not present ");
            throw new RuntimeException(getValidationMessages(","));
        }

        sample.set(new Sample.Builder(name.get()).withRelease(release.get()).build());

        Map<String, String> characteristics = mappedSample.entrySet()
                .stream()
                .filter(entry -> entry.getKey()
                        .startsWith("Characteristic"))
                .collect(Collectors.toMap(Map.Entry::getKey, e -> (String) e.getValue()));

        sample.set(handleCharacteristics(characteristics, sample.get()));
        return sample.get();
    }

    private String getValidationMessages(String delimeter) {
        return String.join(delimeter, validationResult.getValidationMessagesList());
    }

    private Sample parseRelationships(Map<String, Object> mappedSample, Sample sample) {
        Map<String, String> relationships = mappedSample.entrySet()
                .stream()
                .filter(entry -> entry.getKey()
                        .startsWith("Relationship"))
                .collect(Collectors.toMap(Map.Entry::getKey,
                        e -> (String) e.getValue(), (u, v) -> u,
                        LinkedHashMap::new));

        return handleRelationships(relationships, sample);
    }

    private Sample handleRelationships(Map<String, String> relationships, Sample sample) {
        List<Relationship> relationshipList = relationships.values()
                .stream()
                .map(String::trim)
                .map(relationshipTrimmedValue -> Relationship.build(sample.getAccession(),
                        relationshipTrimmedValue.substring(0, relationshipTrimmedValue.indexOf("#")),
                        relationshipTrimmedValue.substring(relationshipTrimmedValue.indexOf("#") + 1)))
                .collect(Collectors.toList());

        return Sample.Builder.fromSample(sample).withRelationships(relationshipList).build();
    }

    private Sample handleCharacteristics(Map<String, String> characteristics, Sample sample) {
        List<Attribute> attributes = new ArrayList<>();
        characteristics.forEach((key, value) -> attributes.add
                (new Attribute.Builder(trimmedKey(key), value).build()));

        return Sample.Builder.fromSample(sample).withAttributes(attributes).build();
    }

    private String trimmedKey(String key) {
        return key.substring(key.indexOf("[") + 1, key.indexOf("]"));
    }

    public List<Map<?, ?>> readObjectsFromCsv(File file) throws IOException {
        CsvSchema bootstrap = CsvSchema.emptySchema().withHeader();
        CsvMapper csvMapper = new CsvMapper();
        csvMapper.configure(CsvParser.Feature.TRIM_SPACES, true)
                .configure(CsvParser.Feature.SKIP_EMPTY_LINES, true)
                .configure(CsvParser.Feature.ALLOW_TRAILING_COMMA, true)
                .configure(CsvParser.Feature.IGNORE_TRAILING_UNMAPPABLE, true)
                .configure(CsvParser.Feature.INSERT_NULLS_FOR_MISSING_COLUMNS, true);
        MappingIterator<Map<?, ?>> mappingIterator = csvMapper.readerFor(Map.class).with(bootstrap).readValues(file);

        return mappingIterator.readAll();
    }

    public String writeAsJson(List<Map<?, ?>> data) throws IOException {
        return objectMapper.writeValueAsString(data);
    }
}
