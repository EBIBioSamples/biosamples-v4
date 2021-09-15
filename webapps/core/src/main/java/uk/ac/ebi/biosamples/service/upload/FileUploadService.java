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
package uk.ac.ebi.biosamples.service.upload;

import com.google.common.collect.Multimap;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.csv.CSVParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import uk.ac.ebi.biosamples.model.Contact;
import uk.ac.ebi.biosamples.model.ExternalReference;
import uk.ac.ebi.biosamples.model.Relationship;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.mongo.model.MongoFileUpload;
import uk.ac.ebi.biosamples.mongo.repo.MongoFileUploadRepository;
import uk.ac.ebi.biosamples.mongo.util.BioSamplesFileUploadSubmissionStatus;
import uk.ac.ebi.biosamples.service.SampleService;
import uk.ac.ebi.biosamples.service.security.BioSamplesAapService;
import uk.ac.ebi.biosamples.service.security.BioSamplesWebinAuthenticationService;
import uk.ac.ebi.biosamples.service.upload.exception.UploadInvalidException;
import uk.ac.ebi.biosamples.utils.upload.Characteristics;
import uk.ac.ebi.biosamples.utils.upload.FileUploadUtils;
import uk.ac.ebi.biosamples.utils.upload.ValidationResult;
import uk.ac.ebi.biosamples.validation.SchemaValidationService;

@Service
public class FileUploadService {
  private Logger log = LoggerFactory.getLogger(getClass());
  private FileUploadUtils fileUploadUtils;
  private ValidationResult validationResult;

  @Autowired private SampleService sampleService;

  @Autowired private SchemaValidationService schemaValidationService;

  @Autowired private BioSamplesWebinAuthenticationService bioSamplesWebinAuthenticationService;

  @Autowired private MongoFileUploadRepository mongoFileUploadRepository;

  @Autowired private FileQueueService fileQueueService;

  public synchronized File upload(
      final MultipartFile file,
      final String aapDomain,
      final String checklist,
      final String webinId,
      final FileUploadUtils fileUploadUtils) {
    this.validationResult = new ValidationResult();
    this.fileUploadUtils = fileUploadUtils;
    final String authProvider = isWebin(isWebinIdUsedToAuthenticate(webinId));
    final boolean isWebin = authProvider.equals(FileUploadUtils.WEBIN_AUTH);

    try {
      final Path temp = Files.createTempFile("upload", ".tsv");

      File fileToBeUploaded = temp.toFile();
      file.transferTo(fileToBeUploaded);

      log.info("Input file name " + fileToBeUploaded.getName());

      final FileReader fr = new FileReader(fileToBeUploaded);
      final BufferedReader reader = new BufferedReader(fr);

      final CSVParser csvParser = this.fileUploadUtils.buildParser(reader);
      final List<String> headers = csvParser.getHeaderNames();

      validateHeaderPositions(headers);

      final List<Multimap<String, String>> csvDataMap =
          this.fileUploadUtils.getCSVDataInMap(csvParser);
      final int numSamples = csvDataMap.size();

      log.info("CSV data size: " + numSamples);

      if (numSamples > 200) {
        log.info("File sample count exceeds limits - queueing file for async submission");
        final String submissionId = fileQueueService.queueFile(file, aapDomain, checklist, webinId);

        return this.fileUploadUtils.writeQueueMessageToFile(submissionId);
      }

      final List<Sample> samples =
          buildSamples(csvDataMap, aapDomain, webinId, checklist, validationResult, isWebin);

      final String persistenceMessage = "Number of samples persisted: " + samples.size();

      log.info("Persistence message: " + persistenceMessage);
      validationResult.addValidationMessage(persistenceMessage);
      log.info("Final message: " + String.join("\n", validationResult.getValidationMessagesList()));

      return this.fileUploadUtils.writeToFile(fileToBeUploaded, headers, samples, validationResult);
    } catch (final Exception e) {
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

  private void validateHeaderPositions(final List<String> headers) {
    if (headers.size() > 0) {
      if ((!headers.get(0).equalsIgnoreCase("Source Name")
          && (!headers.get(1).equalsIgnoreCase("Sample Name"))
          && (!headers.get(2).equalsIgnoreCase("Release Date")))) {
        validationResult.addValidationMessage(
            "ISA tab file must have Source Name as first column, followed by Sample Name and Release Date.");

        throw new UploadInvalidException(
            String.join("\n", validationResult.getValidationMessagesList()));
      }
    }
  }

  private List<Sample> buildSamples(
      final List<Multimap<String, String>> csvDataMap,
      final String aapDomain,
      final String webinId,
      final String checklist,
      final ValidationResult validationResult,
      final boolean isWebin) {
    final Map<String, String> sampleNameToAccessionMap = new LinkedHashMap<>();
    final Map<Sample, Multimap<String, String>> sampleToMappedSample = new LinkedHashMap<>();

    csvDataMap.forEach(
        csvRecordMap -> {
          Sample sample = null;

          try {
            sample =
                buildSample(csvRecordMap, aapDomain, webinId, checklist, validationResult, isWebin);

            if (sample == null) {
              validationResult.addValidationMessage(
                  "Failed to create sample in the file with sample name "
                      + fileUploadUtils.getSampleName(csvRecordMap));
            }
          } catch (Exception e) {
            validationResult.addValidationMessage(
                "Failed to create sample in the file with sample name "
                    + fileUploadUtils.getSampleName(csvRecordMap));
          }

          if (sample != null) {
            sampleNameToAccessionMap.put(sample.getName(), sample.getAccession());
            sampleToMappedSample.put(sample, csvRecordMap);
          }
        });

    return addRelationshipsAndThenBuildSamples(
        sampleNameToAccessionMap, sampleToMappedSample, validationResult, isWebin);
  }

  private boolean isWebinIdUsedToAuthenticate(final String webinId) {
    return webinId != null && webinId.toUpperCase().startsWith(FileUploadUtils.WEBIN_AUTH);
  }

  private List<Sample> addRelationshipsAndThenBuildSamples(
      final Map<String, String> sampleNameToAccessionMap,
      final Map<Sample, Multimap<String, String>> sampleToMappedSample,
      final ValidationResult validationResult,
      final boolean isWebin) {
    return sampleToMappedSample.entrySet().stream()
        .map(
            sampleMultimapEntry ->
                addRelationshipAndThenBuildSample(
                    sampleNameToAccessionMap, sampleMultimapEntry, validationResult, isWebin))
        .collect(Collectors.toList());
  }

  private Sample addRelationshipAndThenBuildSample(
      final Map<String, String> sampleNameToAccessionMap,
      final Map.Entry<Sample, Multimap<String, String>> sampleMultimapEntry,
      final ValidationResult validationResult,
      final boolean isWebin) {
    final Multimap<String, String> relationshipMap =
        fileUploadUtils.parseRelationships(sampleMultimapEntry.getValue());
    Sample sample = sampleMultimapEntry.getKey();
    final List<Relationship> relationships =
        fileUploadUtils.createRelationships(
            sample, sampleNameToAccessionMap, relationshipMap, validationResult);

    relationships.forEach(relationship -> log.info(relationship.toString()));

    sample = Sample.Builder.fromSample(sample).withRelationships(relationships).build();
    sample = storeSample(sample, false, isWebin(isWebin));

    return sample;
  }

  private Sample buildSample(
      final Multimap<String, String> multiMap,
      final String aapDomain,
      final String webinId,
      final String checklist,
      final ValidationResult validationResult,
      final boolean isWebin) {
    final String sampleName = fileUploadUtils.getSampleName(multiMap);
    final String sampleReleaseDate = fileUploadUtils.getReleaseDate(multiMap);
    final String accession = fileUploadUtils.getSampleAccession(multiMap);
    final List<Characteristics> characteristicsList =
        fileUploadUtils.handleCharacteristics(multiMap);
    final List<ExternalReference> externalReferenceList =
        fileUploadUtils.handleExternalReferences(multiMap);
    final List<Contact> contactsList = fileUploadUtils.handleContacts(multiMap);
    boolean isValidatedAgainstChecklist;

    if (fileUploadUtils.isValidSample(sampleName, sampleReleaseDate, validationResult)) {
      Sample sample =
          fileUploadUtils.buildSample(
              sampleName,
              accession,
              sampleReleaseDate,
              characteristicsList,
              externalReferenceList,
              contactsList);

      sample = handleAuthentication(aapDomain, webinId, isWebin, sample, validationResult);

      if (sample != null) {
        sample = fileUploadUtils.addChecklistAttributeAndBuildSample(checklist, sample);

        isValidatedAgainstChecklist = performChecklistValidation(sample);

        if (isValidatedAgainstChecklist) {
          final boolean isFirstTimeMetadataAdded = sampleService.beforeStore(sample, false);
          sample = storeSample(sample, isFirstTimeMetadataAdded, isWebin(isWebin));
          log.info(
              "Sample " + sample.getName() + " created with accession " + sample.getAccession());

          return sample;
        } else {
          validationResult.addValidationMessage(
              sampleName + " failed validation against " + checklist);

          return null;
        }
      } else {
        return null;
      }
    } else {
      return null;
    }
  }

  private Sample handleAuthentication(
      final String aapDomain,
      final String webinId,
      final boolean isWebin,
      Sample sample,
      final ValidationResult validationResult) {
    try {
      if (isWebin) {
        sample = bioSamplesWebinAuthenticationService.handleWebinUser(sample, webinId);
      } else {
        sample = Sample.Builder.fromSample(sample).withDomain(aapDomain).build();
        // sample = bioSamplesAapService.handleSampleDomain(sample);
      }

      return sample;
    } catch (final Exception e) {
      if (e instanceof BioSamplesWebinAuthenticationService.SampleNotAccessibleException) {
        validationResult.addValidationMessage(
            "Sample " + sample.getName() + " is not accessible by your user");
      } else if (e
          instanceof BioSamplesWebinAuthenticationService.WebinUserLoginUnauthorizedException) {
        validationResult.addValidationMessage(
            "Sample " + sample.getName() + " not persisted as WEBIN user is not authorized");
      } else if (e instanceof BioSamplesAapService.SampleDomainMismatchException) {
        validationResult.addValidationMessage(
            "Sample " + sample.getName() + " is not accessible by your user");
      } else if (e instanceof BioSamplesAapService.SampleNotAccessibleException) {
        validationResult.addValidationMessage(
            "Sample " + sample.getName() + " is not accessible by your user");
      } else {
        validationResult.addValidationMessage("General auth error, please retry");
      }

      return null;
    }
  }

  private String isWebin(final boolean isWebin) {
    return isWebin ? FileUploadUtils.WEBIN_AUTH : FileUploadUtils.AAP;
  }

  private Sample storeSample(
      final Sample sample, final boolean isFirstTimeMetadataAdded, final String authProvider) {
    return sampleService.store(sample, isFirstTimeMetadataAdded, authProvider);
  }

  private boolean performChecklistValidation(final Sample sample) {
    boolean isValidatedAgainstChecklist = false;

    try {
      schemaValidationService.validate(sample);
      isValidatedAgainstChecklist = true;
    } catch (final Exception schemaValidationException) {
      log.info("Schema validator failed to validate sample");
    }

    return isValidatedAgainstChecklist;
  }

  public MongoFileUpload getSamples(final String submissionId) {
    final MongoFileUpload mongoFileUpload = mongoFileUploadRepository.findOne(submissionId);

    if (mongoFileUpload != null) {
      return mongoFileUpload;
    } else {
      return new MongoFileUpload(
          submissionId,
          BioSamplesFileUploadSubmissionStatus.NOT_FOUND,
          null,
          null,
          false,
          Collections.emptyList(),
          "Submission not found, please enter a valid submission ID.");
    }
  }

  public List<MongoFileUpload> getUserSubmissions(final List<String> userRoles) {
    try {
      final Pageable page = new PageRequest(0, 10);
      return mongoFileUploadRepository.findBySubmitterDetailsIn(userRoles, page);
    } catch (final Exception e) {
      log.info("Failed in fetch submissions in getUserSubmissions() " + e.getMessage());
      throw new RuntimeException(e);
    }
  }
}
