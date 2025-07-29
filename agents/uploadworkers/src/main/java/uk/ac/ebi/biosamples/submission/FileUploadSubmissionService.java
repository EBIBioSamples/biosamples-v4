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
package uk.ac.ebi.biosamples.submission;

import com.google.common.collect.Multimap;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.commons.csv.CSVParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.core.model.Relationship;
import uk.ac.ebi.biosamples.core.model.Sample;
import uk.ac.ebi.biosamples.exception.GlobalExceptions;
import uk.ac.ebi.biosamples.messaging.MessagingConstants;
import uk.ac.ebi.biosamples.model.SubmissionFile;
import uk.ac.ebi.biosamples.mongo.model.MongoFileUpload;
import uk.ac.ebi.biosamples.mongo.repository.MongoFileUploadRepository;
import uk.ac.ebi.biosamples.mongo.repository.MongoSampleRepository;
import uk.ac.ebi.biosamples.mongo.util.BioSamplesFileUploadSubmissionStatus;
import uk.ac.ebi.biosamples.mongo.util.SampleNameAccessionPair;
import uk.ac.ebi.biosamples.utils.upload.FileUploadUtils;
import uk.ac.ebi.biosamples.utils.upload.ValidationResult;

@Service
public class FileUploadSubmissionService {
  private final Logger log = LoggerFactory.getLogger(getClass());
  private ValidationResult validationResult;
  private FileUploadUtils fileUploadUtils;

  @Autowired
  @Qualifier("WEBINCLIENT")
  BioSamplesClient bioSamplesWebinClient;

  @Autowired FileUploadStorageService fileUploadStorageService;
  @Autowired MongoFileUploadRepository mongoFileUploadRepository;
  @Autowired MongoSampleRepository mongoSampleRepository;

  @RabbitListener(
      queues = MessagingConstants.UPLOAD_QUEUE,
      containerFactory = "biosamplesFileUploadSubmissionContainerFactory")
  public void receiveMessageFromBioSamplesFileUploaderQueue(final String mongoFileId) {
    handleMessage(mongoFileId);
  }

  private void handleMessage(final String submissionId) {
    final Optional<MongoFileUpload> fileUploadOptional =
        mongoFileUploadRepository.findById(submissionId);
    final MongoFileUpload mongoFileUpload =
        fileUploadOptional.orElseThrow(
            () ->
                new GlobalExceptions.UploadInvalidException(
                    "Could not find file upload record for submissionId: " + submissionId));

    try {
      validationResult = new ValidationResult();
      fileUploadUtils = new FileUploadUtils();

      log.info("Received file with file ID " + submissionId);

      final SubmissionFile submissionFile = fileUploadStorageService.getFile(submissionId);
      final boolean isWebin = mongoFileUpload.isWebin();
      final String checklist = mongoFileUpload.getChecklist();
      final String webinId = mongoFileUpload.getSubmitterDetails();
      final Path temp = Files.createTempFile("upload", ".tsv");

      Files.copy(submissionFile.getStream(), temp, StandardCopyOption.REPLACE_EXISTING);

      final File fileToBeUploaded = temp.toFile();
      final FileReader fr = new FileReader(fileToBeUploaded);
      final BufferedReader reader = new BufferedReader(fr);

      final CSVParser csvParser = fileUploadUtils.buildParser(reader);
      final List<String> headers = csvParser.getHeaderNames();

      fileUploadUtils.validateHeaderPositions(headers, validationResult);

      final List<Multimap<String, String>> csvDataMap =
          fileUploadUtils.getISATABDataInMap(csvParser);

      log.info("CSV data size: " + csvDataMap.size());

      final List<Sample> samples =
          buildAndPersistSamples(csvDataMap, webinId, checklist, validationResult, isWebin);
      final List<SampleNameAccessionPair> accessionsList =
          samples.stream()
              .filter(sample -> sample.getAccession() != null)
              .map(sample -> new SampleNameAccessionPair(sample.getName(), sample.getAccession()))
              .collect(Collectors.toList());
      final String persistenceMessage = "Number of samples persisted: " + accessionsList.size();

      validationResult.addValidationMessage(
          new ValidationResult.ValidationMessage(submissionId, persistenceMessage, false));

      final String joinedValidationMessage =
          validationResult.getValidationMessagesList().stream()
              .map(
                  validationMessage ->
                      validationMessage.getMessageKey() + ":" + validationMessage.getMessageValue())
              .collect(Collectors.joining(" -- "));

      log.info(joinedValidationMessage);

      BioSamplesFileUploadSubmissionStatus bioSamplesFileUploadSubmissionStatus =
          BioSamplesFileUploadSubmissionStatus.COMPLETED;

      if (validationResult.getValidationMessagesList().stream()
          .anyMatch(ValidationResult.ValidationMessage::isError)) {
        bioSamplesFileUploadSubmissionStatus =
            BioSamplesFileUploadSubmissionStatus.COMPLETED_WITH_ERRORS;
      }

      final MongoFileUpload mongoFileUploadCompleted =
          new MongoFileUpload(
              submissionId,
              bioSamplesFileUploadSubmissionStatus,
              mongoFileUpload.getSubmissionDate(),
              FileUploadUtils.formatDateString(LocalDateTime.now()),
              mongoFileUpload.getSubmitterDetails(),
              mongoFileUpload.getChecklist(),
              isWebin,
              accessionsList,
              joinedValidationMessage);

      performFinalActions(submissionId, mongoFileUploadCompleted);
    } catch (final Exception e) {
      final String messageForBsdDevTeam =
          "********FEEDBACK TO BSD DEV TEAM START********"
              + e.getMessage()
              + "********FEEDBACK TO BSD DEV TEAM END********";
      validationResult.addValidationMessage(
          new ValidationResult.ValidationMessage(submissionId, messageForBsdDevTeam, true));

      final MongoFileUpload mongoFileUploadFailed =
          new MongoFileUpload(
              submissionId,
              BioSamplesFileUploadSubmissionStatus.FAILED,
              mongoFileUpload.getSubmissionDate(),
              FileUploadUtils.formatDateString(LocalDateTime.now()),
              mongoFileUpload.getSubmitterDetails(),
              mongoFileUpload.getChecklist(),
              mongoFileUpload.isWebin(),
              mongoFileUpload.getSampleNameAccessionPairs(),
              validationResult.getValidationMessagesList().stream()
                  .map(
                      validationMessage ->
                          validationMessage.getMessageKey()
                              + ":"
                              + validationMessage.getMessageValue())
                  .collect(Collectors.joining(" -- ")));

      performFinalActions(submissionId, mongoFileUploadFailed);
    } finally {
      validationResult.clear();
    }
  }

  private void performFinalActions(
      final String submissionId, final MongoFileUpload mongoFileUpload) {
    try {
      mongoFileUploadRepository.save(mongoFileUpload);
    } catch (final Exception e) {
      final MongoFileUpload mongoFileUploadLite =
          new MongoFileUpload(
              mongoFileUpload.getSubmissionId(),
              mongoFileUpload.getSubmissionStatus(),
              mongoFileUpload.getSubmissionDate(),
              FileUploadUtils.formatDateString(LocalDateTime.now()),
              mongoFileUpload.getSubmitterDetails(),
              mongoFileUpload.getChecklist(),
              mongoFileUpload.isWebin(),
              mongoFileUpload.getSampleNameAccessionPairs(),
              null);

      mongoFileUploadRepository.save(mongoFileUploadLite);
    } finally {
      fileUploadStorageService.deleteFile(submissionId);
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
            sample = buildAndPersistSample(csvRecordMap, webinId, checklist);

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
                    "Failed to create sample in the file " + e.getMessage(),
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
                addRelationshipAndThenBuildAndPersistSample(
                    sampleNameToAccessionMap, sampleMultimapEntry, validationResult, isWebin))
        .collect(Collectors.toList());
  }

  private Sample addRelationshipAndThenBuildAndPersistSample(
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

    if (!relationships.isEmpty()) {
      sample = Sample.Builder.fromSample(sample).withRelationships(relationships).build();

      try {
        sample = bioSamplesWebinClient.persistSampleResource(sample).getContent();
      } catch (final Exception e) {
        validationResult.addValidationMessage(
            new ValidationResult.ValidationMessage(
                sample.getAccession(), "Failed to add relationships for sample", true));
      }
    }

    return sample;
  }

  private Sample buildAndPersistSample(
      final Multimap<String, String> multiMap, final String webinId, final String checklist) {
    final String sampleName = FileUploadUtils.getSampleName(multiMap);
    final String accession = fileUploadUtils.getSampleAccession(multiMap);
    final boolean sampleWithAccession = accession != null;
    boolean persisted = true;

    Sample sample = fileUploadUtils.buildSample(multiMap, validationResult);

    if (sample != null) {
      sample = fileUploadUtils.addChecklistAttributeAndBuildSample(checklist, sample);

      try {
        sample = Sample.Builder.fromSample(sample).withWebinSubmissionAccountId(webinId).build();

        /*if (mongoSampleRepository
                    .findByWebinSubmissionAccountIdAndName(webinId, sampleName)
                    .size()
                > 0
            && !sampleWithAccession) {
          validationResult.addValidationMessage(
              new ValidationResult.ValidationMessage(
                  sampleName, " Already exists with submission account " + webinId));

          return null;
        } else {*/
        sample = bioSamplesWebinClient.persistSampleResource(sample).getContent();
        /*}*/
      } catch (final Exception e) {
        persisted = false;
        handleUnauthorizedWhilePersistence(sampleName, accession, sampleWithAccession, e);
      }

      if (sampleWithAccession && persisted) {
        assert sample != null;

        validationResult.addValidationMessage(
            new ValidationResult.ValidationMessage(sample.getAccession(), "Sample updated", false));

        log.info("Updated sample " + sample.getAccession());

        return sample;
      } else if (!sampleWithAccession && persisted) {
        assert sample != null;

        validationResult.addValidationMessage(
            new ValidationResult.ValidationMessage(
                sample.getAccession(),
                "Sample " + sample.getName() + " created with accession " + sample.getAccession(),
                false));

        log.info("Sample " + sampleName + " created with accession " + sample.getAccession());

        return sample;
      } else {
        return null;
      }
    }

    return null;
  }

  private void handleUnauthorizedWhilePersistence(
      final String sampleName,
      final String accession,
      final boolean sampleWithAccession,
      final Exception e) {
    String validationMessage = "";

    if (e.getMessage().contains("403")) {
      validationMessage =
          validationMessage
              + "Error in persisting sample, sample "
              + accession
              + " is not accessible by you";
    }

    validationResult.addValidationMessage(
        new ValidationResult.ValidationMessage(
            sampleWithAccession ? accession : sampleName, validationMessage, true));
  }
}
