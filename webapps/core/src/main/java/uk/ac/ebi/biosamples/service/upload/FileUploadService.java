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
import com.google.common.collect.Multimap;
import org.apache.commons.csv.CSVParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import uk.ac.ebi.biosamples.model.*;
import uk.ac.ebi.biosamples.service.BioSamplesAapService;
import uk.ac.ebi.biosamples.service.BioSamplesWebinAuthenticationService;
import uk.ac.ebi.biosamples.service.SampleService;
import uk.ac.ebi.biosamples.service.SchemaValidationService;
import uk.ac.ebi.biosamples.service.certification.CertifyService;
import uk.ac.ebi.biosamples.service.upload.exception.UploadInvalidException;
import uk.ac.ebi.biosamples.utils.upload.Characteristics;
import uk.ac.ebi.biosamples.utils.upload.FileUploadUtils;
import uk.ac.ebi.biosamples.utils.upload.ValidationResult;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class FileUploadService {
  private Logger log = LoggerFactory.getLogger(getClass());
  private FileUploadUtils fileUploadUtils;
  private ValidationResult validationResult;

  @Autowired ObjectMapper objectMapper;

  @Autowired SampleService sampleService;

  @Autowired CertifyService certifyService;

  @Autowired SchemaValidationService schemaValidationService;

  @Autowired JsonSchemaStoreAccessibilityCheckService jsonSchemaStoreAccessibilityCheckService;

  @Autowired
  BioSamplesWebinAuthenticationService bioSamplesWebinAuthenticationService;

  @Autowired
  BioSamplesAapService bioSamplesAapService;

  public synchronized File upload(
      MultipartFile file, String aapDomain, String checklist, String webinId) {
    validationResult = new ValidationResult();
    fileUploadUtils = new FileUploadUtils();
    final String authProvider = isWebin(isWebinIdUsedToAuthenticate(webinId));
    boolean isWebin = authProvider.equals("WEBIN");

    try {
      Path temp = Files.createTempFile("upload", ".tsv");

      File fileToBeUploaded = temp.toFile();
      file.transferTo(fileToBeUploaded);

      log.info("Input file name " + fileToBeUploaded.getName());

      FileReader fr = new FileReader(fileToBeUploaded);
      BufferedReader reader = new BufferedReader(fr);

      final CSVParser csvParser = fileUploadUtils.buildParser(reader);
      final List<String> headers = csvParser.getHeaderNames();
      final List<Multimap<String, String>> csvDataMap = fileUploadUtils.getCSVDataInMap(csvParser);
      log.info("CSV data size: " + csvDataMap.size());

      final List<Sample> samples = buildSamples(csvDataMap, aapDomain, webinId, checklist, validationResult, isWebin);

      final String persistenceMessage = "Number of samples persisted: " + samples.size();

      log.info("Persistence message: " + persistenceMessage);
      validationResult.addValidationMessage(persistenceMessage);
      log.info("Final message: " + String.join("\n", validationResult.getValidationMessagesList()));

      return fileUploadUtils.writeToFile(fileToBeUploaded, headers, samples, validationResult);
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

  private List<Sample> buildSamples(
          List<Multimap<String, String>> csvDataMap,
          String aapDomain,
          String webinId,
          String checklist,
          ValidationResult validationResult,
          boolean isWebin) {
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
                    csvRecordMap, aapDomain, webinId, checklist, isSchemaValidatorAccessible, validationResult, isWebin);

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
        sampleNameToAccessionMap, sampleToMappedSample, validationResult, isWebin);
  }

  private boolean isWebinIdUsedToAuthenticate(String webinId) {
    return webinId != null && webinId.toUpperCase().startsWith("WEBIN");
  }

  private List<Sample> addRelationshipsAndThenBuildSamples(
          Map<String, String> sampleNameToAccessionMap,
          Map<Sample, Multimap<String, String>> sampleToMappedSample,
          ValidationResult validationResult,
          boolean isWebin) {
    return sampleToMappedSample.entrySet().stream()
        .map(
            sampleMultimapEntry ->
                addRelationshipAndThenBuildSample(
                    sampleNameToAccessionMap, sampleMultimapEntry, validationResult, isWebin))
        .collect(Collectors.toList());
  }

  private Sample addRelationshipAndThenBuildSample(
          Map<String, String> sampleNameToAccessionMap,
          Map.Entry<Sample, Multimap<String, String>> sampleMultimapEntry,
          ValidationResult validationResult, boolean isWebin) {
    final Multimap<String, String> relationshipMap =
        fileUploadUtils.parseRelationships(sampleMultimapEntry.getValue());
    Sample sample = sampleMultimapEntry.getKey();
    final List<Relationship> relationships =
        fileUploadUtils.createRelationships(sample, sampleNameToAccessionMap, relationshipMap, validationResult);

    relationships.forEach(relationship -> log.info(relationship.toString()));

    sample = Sample.Builder.fromSample(sample).withRelationships(relationships).build();
    sample = storeSample(sample, false, isWebin(isWebin));

    return sample;
  }

  private Sample buildSample(
          Multimap<String, String> multiMap,
          String aapDomain,
          String webinId,
          String certificate,
          boolean isSchemaValidatorAccessible, ValidationResult validationResult,
          boolean isWebin)
      throws JsonProcessingException {
    final String sampleName = fileUploadUtils.getSampleName(multiMap);
    final String sampleReleaseDate = fileUploadUtils.getReleaseDate(multiMap);
    final String accession = fileUploadUtils.getSampleAccession(multiMap);
    final List<Characteristics> characteristicsList = fileUploadUtils.handleCharacteristics(multiMap);
    final List<ExternalReference> externalReferenceList = fileUploadUtils.handleExternalReferences(multiMap);
    final List<Contact> contactsList = fileUploadUtils.handleContacts(multiMap);
    boolean isCertified;

    if (fileUploadUtils.isBasicValidationFailure(sampleName, sampleReleaseDate, validationResult)) {
      Sample sample = fileUploadUtils.buildSample(sampleName, accession, characteristicsList, externalReferenceList, contactsList);

      sample = handleAuthentication(aapDomain, webinId, isWebin, sample, validationResult);

      if (sample != null) {

        sample = fileUploadUtils.addChecklistAttributeAndBuildSample(certificate, sample);

        isCertified = validateSample(certificate, isSchemaValidatorAccessible, sample);

        if (isCertified) {
          final boolean isFirstTimeMetadataAdded = sampleService.beforeStore(sample, false);
          sample = storeSample(sample, isFirstTimeMetadataAdded, isWebin(isWebin));
          log.info("Sample " + sample.getName() + " created with accession " + sample.getAccession());

          return sample;
        } else {
          validationResult.addValidationMessage(
                  sampleName + " failed validation against " + certificate);

          return null;
        }
      } else {
        return null;
      }
    } else {
      return null;
    }
  }

  private Sample handleAuthentication(String aapDomain, String webinId, boolean isWebin, Sample sample, ValidationResult validationResult) {
    try {
      if (isWebin) {
        sample = bioSamplesWebinAuthenticationService.handleWebinUser(sample, webinId);
      } else {
        sample = Sample.Builder.fromSample(sample).withDomain(aapDomain).build();
        //sample = bioSamplesAapService.handleSampleDomain(sample);
      }

      return sample;
    } catch (final Exception e) {
      if (e instanceof BioSamplesWebinAuthenticationService.SampleNotAccessibleException) {
        validationResult.addValidationMessage("Sample " + sample.getName() + " is not accessible by your user");
      } else if (e instanceof BioSamplesWebinAuthenticationService.WebinUserLoginUnauthorizedException) {
        validationResult.addValidationMessage("Sample " + sample.getName() + " not persisted as WEBIN user is not authorized");
      } else if (e instanceof BioSamplesAapService.SampleDomainMismatchException) {
        validationResult.addValidationMessage("Sample " + sample.getName() + " is not accessible by your user");
      } else if (e instanceof BioSamplesAapService.SampleNotAccessibleException) {
        validationResult.addValidationMessage("Sample " + sample.getName() + " is not accessible by your user");
      } else {
        validationResult.addValidationMessage("General auth error, please retry");
      }

      return null;
    }
  }

  private String isWebin(boolean isWebin) {
    return isWebin ? "WEBIN" : "AAP";
  }

  private Sample storeSample(Sample sample, boolean isFirstTimeMetadataAdded, String authProvider) {
    return sampleService.store(sample, isFirstTimeMetadataAdded, authProvider);
  }

  private boolean validateSample(String certificate, boolean isSchemaValidatorAccessible, Sample sample) throws JsonProcessingException {
    boolean isCertified;

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
    return isCertified;
  }

  private boolean certify(Sample sample, String certificate) throws JsonProcessingException {
    List<Certificate> certificates =
        certifyService.certify(objectMapper.writeValueAsString(sample), false, certificate);

    return certificates.size() != 0;
  }
}
