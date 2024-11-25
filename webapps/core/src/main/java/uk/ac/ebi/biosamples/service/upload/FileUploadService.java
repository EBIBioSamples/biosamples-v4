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
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.commons.csv.CSVParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import uk.ac.ebi.biosamples.exceptions.GlobalExceptions;
import uk.ac.ebi.biosamples.model.Relationship;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.mongo.model.MongoFileUpload;
import uk.ac.ebi.biosamples.mongo.repository.MongoFileUploadRepository;
import uk.ac.ebi.biosamples.mongo.util.BioSamplesFileUploadSubmissionStatus;
import uk.ac.ebi.biosamples.mongo.util.SampleNameAccessionPair;
import uk.ac.ebi.biosamples.service.SampleService;
import uk.ac.ebi.biosamples.service.security.WebinAuthenticationService;
import uk.ac.ebi.biosamples.utils.upload.FileUploadUtils;
import uk.ac.ebi.biosamples.utils.upload.ValidationResult;
import uk.ac.ebi.biosamples.validation.SchemaValidationService;

@Service
public class FileUploadService {
  private final Logger log = LoggerFactory.getLogger(getClass());

  private final SampleService sampleService;
  private final SchemaValidationService schemaValidationService;
  private final WebinAuthenticationService webinAuthenticationService;
  private final MongoFileUploadRepository mongoFileUploadRepository;
  private final FileQueueService fileQueueService;
  private final FileUploadUtils fileUploadUtils;

  public FileUploadService(
      SampleService sampleService,
      SchemaValidationService schemaValidationService,
      WebinAuthenticationService webinAuthenticationService,
      MongoFileUploadRepository mongoFileUploadRepository,
      FileQueueService fileQueueService,
      FileUploadUtils fileUploadUtils) {
    this.fileUploadUtils = fileUploadUtils;
    this.sampleService = sampleService;
    this.schemaValidationService = schemaValidationService;
    this.webinAuthenticationService = webinAuthenticationService;
    this.mongoFileUploadRepository = mongoFileUploadRepository;
    this.fileQueueService = fileQueueService;
  }

  public synchronized File upload(
      final MultipartFile file,
      final String checklist,
      final String webinId,
      final FileUploadUtils fileUploadUtils) {
    final ValidationResult validationResult = new ValidationResult();
    final boolean isWebin = webinId != null && webinId.toUpperCase().startsWith("WEBIN");
    final String uniqueUploadId = UUID.randomUUID().toString();
    String submissionDate = FileUploadUtils.formatDateString(LocalDateTime.now());

    try {
      persistSubmissionState(
          checklist,
          webinId,
          uniqueUploadId,
          BioSamplesFileUploadSubmissionStatus.ACTIVE,
          submissionDate,
          null,
          null,
          false);

      final Path temp = Files.createTempFile("upload", ".tsv");
      final File fileToBeUploaded = temp.toFile();

      file.transferTo(fileToBeUploaded);

      log.info("Input file name " + fileToBeUploaded.getName());

      final FileReader fr = new FileReader(fileToBeUploaded);
      final BufferedReader reader = new BufferedReader(fr);
      final CSVParser csvParser = this.fileUploadUtils.buildParser(reader);
      final List<String> headers = csvParser.getHeaderNames();

      fileUploadUtils.validateHeaderPositions(headers, validationResult);

      final List<Multimap<String, String>> csvDataMap =
          fileUploadUtils.getISATABDataInMap(csvParser);
      final int numSamples = csvDataMap.size();

      log.info("CSV data size: " + numSamples);

      if (numSamples > 200) {
        log.info("File sample count exceeds limits - queueing file for async submission");

        final String submissionId =
            fileQueueService.queueFileinMongoAndSendMessageToRabbitMq(file, checklist, webinId);

        return fileUploadUtils.writeQueueMessageToFile(submissionId);
      }

      final List<Sample> samples =
          buildAndPersistSamples(csvDataMap, webinId, checklist, validationResult, isWebin);
      final List<SampleNameAccessionPair> accessionsList =
          samples.stream()
              .filter(sample -> sample.getAccession() != null)
              .map(sample -> new SampleNameAccessionPair(sample.getName(), sample.getAccession()))
              .toList();
      final String persistenceMessage = "Number of samples persisted: " + accessionsList.size();

      log.info("Persistence message: " + persistenceMessage);

      validationResult.addValidationMessage(
          new ValidationResult.ValidationMessage(uniqueUploadId, persistenceMessage, false));

      log.info(
          "Final message: "
              + validationResult.getValidationMessagesList().stream()
                  .map(
                      validationMessage ->
                          validationMessage.getMessageKey()
                              + ":"
                              + validationMessage.getMessageValue())
                  .collect(Collectors.joining("\n")));

      BioSamplesFileUploadSubmissionStatus bioSamplesFileUploadSubmissionStatus =
          BioSamplesFileUploadSubmissionStatus.COMPLETED;

      if (validationResult.getValidationMessagesList().stream()
          .anyMatch(ValidationResult.ValidationMessage::isError)) {
        bioSamplesFileUploadSubmissionStatus =
            BioSamplesFileUploadSubmissionStatus.COMPLETED_WITH_ERRORS;
      }

      persistSubmissionState(
          checklist,
          webinId,
          uniqueUploadId,
          bioSamplesFileUploadSubmissionStatus,
          submissionDate,
          validationResult.getValidationMessagesList().stream()
              .map(
                  validationMessage ->
                      validationMessage.getMessageKey() + ":" + validationMessage.getMessageValue())
              .collect(Collectors.joining(" -- ")),
          accessionsList,
          true);

      return fileUploadUtils.writeToFile(fileToBeUploaded, headers, samples, validationResult);
    } catch (final Exception e) {
      final String messageForBsdDevTeam =
          "********FEEDBACK TO BSD DEV TEAM START********"
              + e.getMessage()
              + "********FEEDBACK TO BSD DEV TEAM END********";
      validationResult.addValidationMessage(
          new ValidationResult.ValidationMessage(uniqueUploadId, messageForBsdDevTeam, true));
      throw new GlobalExceptions.UploadInvalidException(
          validationResult.getValidationMessagesList().stream()
              .map(
                  validationMessage ->
                      validationMessage.getMessageKey() + ":" + validationMessage.getMessageValue())
              .collect(Collectors.joining("\n")));
    } finally {
      validationResult.clear();
    }
  }

  private void persistSubmissionState(
      final String checklist,
      final String webinId,
      final String uniqueUploadId,
      final BioSamplesFileUploadSubmissionStatus bioSamplesFileUploadSubmissionStatus,
      final String submissionDate,
      final String validationResult,
      final List<SampleNameAccessionPair> accessionPairs,
      final boolean isUpdate) {
    final MongoFileUpload mongoFileUpload =
        new MongoFileUpload(
            uniqueUploadId,
            bioSamplesFileUploadSubmissionStatus,
            submissionDate,
            FileUploadUtils.formatDateString(LocalDateTime.now()),
            webinId,
            checklist,
            true,
            accessionPairs,
            validationResult);

    if (isUpdate) {
      mongoFileUploadRepository.save(mongoFileUpload);
    } else {
      mongoFileUploadRepository.insert(mongoFileUpload);
    }
  }

  private List<Sample> buildAndPersistSamples(
      final List<Multimap<String, String>> csvDataMap,
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
            sample = buildAndPersistSample(csvRecordMap, webinId, checklist, validationResult);

            if (sample == null) {
              validationResult.addValidationMessage(
                  new ValidationResult.ValidationMessage(
                      fileUploadUtils.getSampleName(csvRecordMap),
                      "Failed to create sample in the file",
                      true));
            }
          } catch (final Exception e) {
            validationResult.addValidationMessage(
                new ValidationResult.ValidationMessage(
                    fileUploadUtils.getSampleName(csvRecordMap),
                    "Failed to create sample in the file",
                    true));
          }

          if (sample != null) {
            sampleNameToAccessionMap.put(sample.getName(), sample.getAccession());
            sampleToMappedSample.put(sample, csvRecordMap);
          }
        });

    return addRelationshipsAndThenBuildSamples(
        sampleNameToAccessionMap, sampleToMappedSample, validationResult, isWebin);
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
                    sampleNameToAccessionMap, sampleMultimapEntry, validationResult))
        .collect(Collectors.toList());
  }

  private Sample addRelationshipAndThenBuildSample(
      final Map<String, String> sampleNameToAccessionMap,
      final Map.Entry<Sample, Multimap<String, String>> sampleMultimapEntry,
      final ValidationResult validationResult) {
    final Multimap<String, String> relationshipMap =
        fileUploadUtils.parseRelationships(sampleMultimapEntry.getValue());
    Sample sample = sampleMultimapEntry.getKey();
    Optional<Sample> oldSample = Optional.empty();

    final List<Relationship> relationships =
        fileUploadUtils.createRelationships(
            sample, sampleNameToAccessionMap, relationshipMap, validationResult);

    if (!sampleService.isNotExistingAccession(sample.getAccession())) {
      // fetch returns sample with curations applied
      oldSample = sampleService.fetch(sample.getAccession(), false);
    }

    if (relationships != null && !relationships.isEmpty()) {
      relationships.forEach(relationship -> log.info(relationship.toString()));

      sample = Sample.Builder.fromSample(sample).withRelationships(relationships).build();
      try {
        sample = storeSample(sample, oldSample);
      } catch (final Exception e) {
        final String sampleName = sample.getName();

        new ValidationResult.ValidationMessage(
            sampleName, sampleName + "failed to persist relationships", true);
      }
    }

    return sample;
  }

  private Sample buildAndPersistSample(
      final Multimap<String, String> multiMap,
      final String webinId,
      final String checklist,
      final ValidationResult validationResult) {
    final String sampleName = FileUploadUtils.getSampleName(multiMap);
    final boolean isValidatedAgainstChecklist;
    boolean sampleWithAccession = false;

    Sample sample = fileUploadUtils.buildSample(multiMap, validationResult);
    Optional<Sample> oldSample = Optional.empty();

    if (sample.getAccession() != null) {
      sampleWithAccession = true;

      if (!sampleService.isNotExistingAccession(sample.getAccession())) {
        // fetch returns sample with curations applied
        oldSample = sampleService.fetch(sample.getAccession(), false);
      }
    }

    if (oldSample.isPresent()) {
      log.info("Old sample is " + oldSample.get());
    } else {
      log.info("Old sample not present");
    }

    sample = handleAuthentication(webinId, sample, oldSample, validationResult);

    if (sample != null) {
      sample = fileUploadUtils.addChecklistAttributeAndBuildSample(checklist, sample);

      isValidatedAgainstChecklist = performChecklistValidation(sample);

      if (isValidatedAgainstChecklist) {
        try {
          sample = storeSample(sample, oldSample);

          if (sample != null) {
            if (sampleWithAccession) {
              log.info("Sample " + sample.getAccession() + " is updated");
            } else {
              log.info(
                  "Sample "
                      + sample.getName()
                      + " created with accession "
                      + sample.getAccession());
            }
          }

          return sample;
        } catch (final Exception e) {
          new ValidationResult.ValidationMessage(
              sampleName, sampleName + "failed to persist", true);

          return null;
        }
      } else {
        validationResult.addValidationMessage(
            new ValidationResult.ValidationMessage(
                sampleName, sampleName + " failed validation against " + checklist, true));

        return null;
      }
    } else {
      return null;
    }
  }

  private Sample handleAuthentication(
      final String webinId,
      Sample sample,
      final Optional<Sample> oldSample,
      final ValidationResult validationResult) {
    try {
      sample = Sample.Builder.fromSample(sample).withWebinSubmissionAccountId(webinId).build();
      sample = webinAuthenticationService.handleWebinUserSubmission(sample, webinId, oldSample);

      return sample;
    } catch (final Exception e) {
      log.info(e.getMessage(), e);

      if (e instanceof GlobalExceptions.SampleNotAccessibleException) {
        validationResult.addValidationMessage(
            new ValidationResult.ValidationMessage(
                sample.getName(),
                "Sample " + sample.getName() + " is not accessible for you",
                true));
      } else if (e instanceof GlobalExceptions.WebinUserLoginUnauthorizedException) {
        validationResult.addValidationMessage(
            new ValidationResult.ValidationMessage(
                sample.getName(),
                "Sample " + sample.getName() + " not persisted as WEBIN user is not authorized",
                true));
      } else if (e instanceof GlobalExceptions.SampleDomainMismatchException) {
        validationResult.addValidationMessage(
            new ValidationResult.ValidationMessage(
                sample.getName(),
                "Sample " + sample.getName() + " is not accessible for you",
                true));
      } else if (e instanceof GlobalExceptions.InvalidSubmissionSourceException) {
        validationResult.addValidationMessage(
            new ValidationResult.ValidationMessage(
                sample.getName(),
                "Sample "
                    + sample.getName()
                    + " has been imported from other INSDC databases, please update at source. Please contact the BioSamples Helpdesk at biosamples@ebi.ac.uk for more information",
                true));
      } else {
        validationResult.addValidationMessage(
            new ValidationResult.ValidationMessage(
                sample.getName(), "Please retry submission!", true));
      }

      return null;
    }
  }

  private Sample storeSample(final Sample sample, final Optional<Sample> oldSample) {
    /*final String sampleName = sample.getName();
    final String accession = sample.getAccession();

    if (authProvider.equals(FileUploadUtils.WEBIN_AUTH)) {
      final String webinSubmissionAccountId = sample.getWebinSubmissionAccountId();

      if (mongoSampleRepository
                  .findByWebinSubmissionAccountIdAndName(webinSubmissionAccountId, sampleName)
                  .size()
              > 0
          && accession == null) {
        validationResult.addValidationMessage(
            new ValidationResult.ValidationMessage(
                sampleName, " Already exists with submission account " + webinSubmissionAccountId));

        return null;
      }
    } else {
      final String domain = sample.getDomain();

      if (mongoSampleRepository.findByDomainAndName(domain, sampleName).size() > 0
          && accession == null) {
        validationResult.addValidationMessage(
            new ValidationResult.ValidationMessage(
                sampleName, " Already exists with submission account " + domain));

        return null;
      }
    }*/

    try {
      return sampleService.persistSample(sample, oldSample.orElse(null), false);
    } catch (final Exception e) {
      throw new RuntimeException("Failed to persist sample with name " + sample.getName());
    }
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
    final MongoFileUpload mongoFileUpload =
        mongoFileUploadRepository.findById(submissionId).isPresent()
            ? mongoFileUploadRepository.findById(submissionId).get()
            : null;

    return Objects.requireNonNullElseGet(
        mongoFileUpload,
        () ->
            new MongoFileUpload(
                submissionId,
                BioSamplesFileUploadSubmissionStatus.NOT_FOUND,
                null,
                null,
                null,
                null,
                false,
                Collections.emptyList(),
                "Submission not found, please enter a valid submission ID."));
  }

  public List<MongoFileUpload> getUserSubmissions(final String user) {
    try {
      final Pageable page = PageRequest.of(0, 10);
      return mongoFileUploadRepository.findBySubmitterDetails(user, page);
    } catch (final Exception e) {
      log.info("Failed in fetch submissions in getUserSubmissions() " + e.getMessage());
      throw new RuntimeException(e);
    }
  }
}
