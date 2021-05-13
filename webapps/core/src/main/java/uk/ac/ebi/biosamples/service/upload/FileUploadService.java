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
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvParser;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.Certificate;
import uk.ac.ebi.biosamples.model.Relationship;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.upload.validation.Messages;
import uk.ac.ebi.biosamples.model.upload.validation.ValidationResult;
import uk.ac.ebi.biosamples.service.SampleService;
import uk.ac.ebi.biosamples.service.certification.CertifyService;

@Service
public class FileUploadService {
  private Logger log = LoggerFactory.getLogger(getClass());
  private ValidationResult validationResult = new ValidationResult();

  @Autowired ObjectMapper objectMapper;

  @Autowired SampleService sampleService;

  @Autowired CertifyService certifyService;

  @Autowired Messages messages;

  public File upload(MultipartFile file, String aapDomain, String certificate, String webinId)
      throws IOException {
    Path temp = Files.createTempFile("upload", ".tsv");

    File fileToBeUploaded = temp.toFile();
    file.transferTo(fileToBeUploaded);

    validationResult.clear();
    messages.clear();

    List<Map<?, ?>> data = readObjectsFromCsv(fileToBeUploaded);
    String json = writeAsJson(data);

    log.info("JSON is" + json);

    List<Map<String, Object>> jsonListOfMappedSamples =
        objectMapper.readValue(json, new TypeReference<List<Map<String, Object>>>() {});

    return createSamplesFromMappedData(jsonListOfMappedSamples, aapDomain, certificate, webinId);
  }

  private File createSamplesFromMappedData(
      List<Map<String, Object>> jsonListOfMappedSamples,
      String aapDomain,
      String certificate,
      String webinId)
      throws IOException {
    final Map<Sample, Map<String, Object>> sampleToMappedSample = new LinkedHashMap<>();
    final Map<String, String> sampleNameToAccessionMap = new LinkedHashMap<>();
    final String authProvider = isWebinIdUsedToAuthenticate(webinId) ? "WEBIN" : "AAP";

    jsonListOfMappedSamples.forEach(
        mappedSample -> {
          try {
            Sample sample = validateAndBuildSample(mappedSample);

            log.info("Sample before submission is " + sample.toString());

            if (sampleService.isWebinAuthorization(authProvider)) {
              sample =
                  Sample.Builder.fromSample(sample).withWebinSubmissionAccountId(webinId).build();
            } else {
              sample = Sample.Builder.fromSample(sample).withDomain(aapDomain).build();
            }

            if (certify(sample, certificate)) {
              sample = sampleService.store(sample, true, authProvider);
              sampleNameToAccessionMap.put(sample.getName(), sample.getAccession());
              sampleToMappedSample.put(sample, mappedSample);
            } else {
              validationResult.addValidationMessage(
                  sample.getName() + " failed validation against " + certificate);
            }
          } catch (Exception e) {
            log.info(getValidationMessages("\n"));
          }
        });

    List<Sample> samples =
        sampleToMappedSample.entrySet().stream()
            .map(
                sampleMapEntry ->
                    parseRelationships(
                        sampleMapEntry.getValue(),
                        sampleMapEntry.getKey(),
                        sampleNameToAccessionMap))
            .collect(Collectors.toList());

    samples =
        samples.stream()
            .map(sample -> sampleService.store(sample, true, authProvider))
            .collect(Collectors.toList());

    samples.forEach(
        sample ->
            messages.addMessage(
                "Sample "
                    + sample.getName()
                    + " is assigned accession "
                    + sample.getAccession()
                    + "\n"));

    return writeMessagesAndValidationResultsToFile(messages, validationResult);
  }

  private File writeMessagesAndValidationResultsToFile(
      Messages messages, ValidationResult validationResult) throws IOException {
    messages.getMessagesList().forEach(message -> log.info(message));
    validationResult.getValidationMessagesList().forEach(valResult -> log.info(valResult));

    Path temp = Files.createTempFile("upload_result", ".txt");
    File file = temp.toFile();

    try {
      FileOutputStream writeDataStream = new FileOutputStream(file);
      ObjectOutputStream writeStream = new ObjectOutputStream(writeDataStream);

      writeStream.writeChars("Messages");
      writeStream.writeObject(messages.getMessagesList());
      writeStream.writeChars("Validation messages");
      writeStream.writeObject(validationResult.getValidationMessagesList());

      writeStream.flush();
      writeStream.close();
    } catch (IOException e) {
      log.info(e.getMessage(), e);
    }

    return file;
  }

  private boolean isWebinIdUsedToAuthenticate(String webinId) {
    return webinId != null && webinId.toUpperCase().startsWith("WEBIN");
  }

  private boolean certify(Sample sample, String certificate) throws JsonProcessingException {
    List<Certificate> certificates =
        certifyService.certify(objectMapper.writeValueAsString(sample), false, certificate);

    return certificates.size() != 0;
  }

  private Sample validateAndBuildSample(Map<String, Object> mappedSample) {
    AtomicReference<Sample> sample = new AtomicReference<>();
    AtomicReference<String> name = new AtomicReference<>();
    AtomicReference<String> release = new AtomicReference<>();
    AtomicReference<Attribute> descriptionAttribute = new AtomicReference<>();

    mappedSample.forEach(
        (key, value) -> {
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

          if (key.equalsIgnoreCase("Sample Description")) {
            if (value != null) {
              descriptionAttribute.set(Attribute.build("description", value.toString()));
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

    Map<String, String> characteristics =
        mappedSample.entrySet().stream()
            .filter(entry -> entry.getKey().startsWith("Characteristic"))
            .collect(Collectors.toMap(Map.Entry::getKey, e -> (String) e.getValue()));

    sample.set(handleCharacteristics(characteristics, sample.get(), descriptionAttribute.get()));

    return sample.get();
  }

  private String getValidationMessages(String delimeter) {
    return String.join(delimeter, validationResult.getValidationMessagesList());
  }

  private Sample parseRelationships(
      Map<String, Object> mappedSample,
      Sample sample,
      Map<String, String> sampleNameToAccessionMap) {
    Map<String, String> relationships =
        mappedSample.entrySet().stream()
            .filter(entry -> entry.getKey().startsWith("Relationship"))
            .collect(
                Collectors.toMap(
                    Map.Entry::getKey,
                    e -> (String) e.getValue(),
                    (u, v) -> u,
                    LinkedHashMap::new));

    return handleRelationships(relationships, sample, sampleNameToAccessionMap);
  }

  private Sample handleRelationships(
      Map<String, String> relationships,
      Sample sample,
      Map<String, String> sampleNameToAccessionMap) {
    List<Relationship> relationshipList =
        relationships.values().stream()
            .map(String::trim)
            .filter(
                relationshipTrimmedValue ->
                    relationshipTrimmedValue.contains("#")) /*TODO check something there after #*/
            .map(
                relationshipTrimmedValue ->
                    Relationship.build(
                        sample.getAccession(),
                        relationshipTrimmedValue.substring(
                            0, relationshipTrimmedValue.indexOf("#")),
                        sampleNameToAccessionMap.get(
                            relationshipTrimmedValue.substring(
                                relationshipTrimmedValue.indexOf("#") + 1))))
            .collect(Collectors.toList());

    return Sample.Builder.fromSample(sample).withRelationships(relationshipList).build();
  }

  private Sample handleCharacteristics(
      Map<String, String> characteristics, Sample sample, Attribute descriptionAttribute) {
    List<Attribute> attributes = new ArrayList<>();
    characteristics.forEach(
        (key, value) -> {
          if (keyContainsAnythingApartFromText(key)) {
            final String attributeText = valueMinusEverything(value);
            final String unit = getUnitFromValue(value);

            attributes.add(
                new Attribute.Builder(trimmedKey(key), attributeText)
                    .withUnit(unit)
                    .withTag("attribute")
                    .build());
          } else {
            attributes.add(
                new Attribute.Builder(trimmedKey(key), value).withTag("attribute").build());
          }
        });

    attributes.add(descriptionAttribute);

    return Sample.Builder.fromSample(sample).withAttributes(attributes).build();
  }

  private boolean keyContainsAnythingApartFromText(String key) {
    return key.contains("#");
  }

  private String getUnitFromValue(String value) {
    int index = value.indexOf("#");

    if (index != -1) return value.substring(value.indexOf("#") + 1);
    else return null;
  }

  private String valueMinusEverything(String value) {
    int index = value.indexOf("#");

    if (index != -1) return value.substring(0, value.indexOf("#"));
    else return value;
  }

  private String trimmedKey(String key) {
    return key.substring(key.indexOf("[") + 1, key.indexOf("]"));
  }

  public List<Map<?, ?>> readObjectsFromCsv(File file) throws IOException {
    CsvSchema bootstrap = CsvSchema.emptySchema().withHeader().withColumnSeparator('\t');
    CsvMapper csvMapper = new CsvMapper();
    csvMapper
        .configure(CsvParser.Feature.TRIM_SPACES, true)
        .configure(CsvParser.Feature.SKIP_EMPTY_LINES, true)
        .configure(CsvParser.Feature.ALLOW_TRAILING_COMMA, true)
        .configure(CsvParser.Feature.IGNORE_TRAILING_UNMAPPABLE, true)
        .configure(CsvParser.Feature.INSERT_NULLS_FOR_MISSING_COLUMNS, true);
    MappingIterator<Map<?, ?>> mappingIterator =
        csvMapper.readerFor(Map.class).with(bootstrap).readValues(file);

    return mappingIterator.readAll();
  }

  public String writeAsJson(List<Map<?, ?>> data) throws IOException {
    return objectMapper.writeValueAsString(data);
  }
}
