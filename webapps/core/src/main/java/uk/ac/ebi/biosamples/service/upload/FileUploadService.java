package uk.ac.ebi.biosamples.service.upload;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvParser;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import org.apache.commons.csv.CSVParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.Relationship;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.upload.validation.ValidationResult;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Service
public class FileUploadService {
    private Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    ValidationResult validationResult;

    public void upload(MultipartFile file) throws IOException {
        Path temp = Files.createTempFile("test", ".tmp");

        File fileToBeUploaded = temp.toFile();
        file.transferTo(fileToBeUploaded);

        List<Map<?, ?>> data = readObjectsFromCsv(fileToBeUploaded);
        String json = writeAsJson(data);

        log.info("JSON is" + json);

        List<Map<String, Object>> jsonListOfMappedSamples = objectMapper.readValue(json,
                new TypeReference<List<Map<String, Object>>>() {
                });

        log.info(String.valueOf(jsonListOfMappedSamples.size()));

        createSamplesFromMappedData(jsonListOfMappedSamples);
    }

    private <T> void createSamplesFromMappedData(List<Map<String, Object>> jsonListOfMappedSamples) {
        List<Sample> samples = new ArrayList<>();

        jsonListOfMappedSamples.forEach(mappedSample -> {
            T t = validateAndBuildSample(mappedSample);

            if (t instanceof Sample) {
                samples.add((Sample) t);
            } else if (t instanceof ValidationResult) {
                ValidationResult validationResult = (ValidationResult) t;
                throw new RuntimeException("Spreadsheet validation failed " + String.join("", validationResult.getValidationMessagesList()));
            }
        });

        log.info(String.valueOf(samples.size()));

        samples.forEach(sample -> log.info(sample.toString()));

        jsonListOfMappedSamples.forEach(mappedJson -> {
            mappedJson.keySet().forEach(key -> log.info(key));
            mappedJson.values().forEach(value -> log.info((String) value));
        });
    }

    private <T> T validateAndBuildSample(Map<String, Object> mappedSample) {
        AtomicReference<Sample> sample = new AtomicReference<>();
        AtomicBoolean sampleNamePresent = new AtomicBoolean(false);
        AtomicBoolean sampleReleaseDatePresent = new AtomicBoolean(false);
        AtomicInteger relationshipIncrementer = new AtomicInteger(0);

        mappedSample.forEach((key, value) -> {
            if (key.equalsIgnoreCase("Sample Name")) {
                sample.set(new Sample.Builder(String.valueOf(value)).build());
                sampleNamePresent.set(true);
            }

            if (key.equalsIgnoreCase("Release")) {
                sample.set(Sample.Builder.fromSample(sample.get()).withRelease(String.valueOf(value)).build());
                sampleReleaseDatePresent.set(true);
            }
        });

        Map<String, String> characteristics = mappedSample.entrySet()
                .stream()
                .filter(entry -> entry.getKey()
                        .startsWith("Characteristic"))
                .collect(Collectors.toMap(Map.Entry::getKey, e -> (String) e.getValue()));

        sample.set(handleCharacteristics(characteristics, sample.get()));

        Map<String, String> relationships = mappedSample.entrySet()
                .stream()
                .filter(entry -> entry.getKey()
                        .startsWith("Relationship"))
                .collect(Collectors.toMap(e -> e.getKey() + relationshipIncrementer.getAndIncrement(),
                        e -> (String) e.getValue(), (u, v) -> u,
                        LinkedHashMap::new));

        sample.set(handleRelationships(relationships, sample.get()));

        if (!sampleNamePresent.get()) {
            validationResult.addValidationMessage(" Sample name not present. ");
        }

        if (!sampleReleaseDatePresent.get()) {
            validationResult.addValidationMessage(" Sample release date not present. ");
        }

        if (validationResult.getValidationMessagesList().size() == 0) {
            return (T) sample.get();
        } else {
            return (T) validationResult;
        }
    }

    private Sample handleRelationships(Map<String, String> relationships, Sample sample) {
        List<Relationship> relationshipList = relationships.values()
                .stream()
                .map(relationshipValue -> relationshipValue.trim())
                .map(relationshipTrimmedValue -> Relationship.build("SAMEA11111",
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
        csvMapper.configure(CsvParser.Feature.TRIM_SPACES, true);
        MappingIterator<Map<?, ?>> mappingIterator = csvMapper.readerFor(Map.class).with(bootstrap).readValues(file);

        return mappingIterator.readAll();
    }

    public String writeAsJson(List<Map<?, ?>> data) throws IOException {
        return objectMapper.writeValueAsString(data);
    }
}
