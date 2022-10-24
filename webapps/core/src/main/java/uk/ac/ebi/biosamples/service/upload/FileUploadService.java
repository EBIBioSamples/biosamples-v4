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
import java.util.*;
import java.util.stream.Collectors;
import org.apache.commons.csv.CSVParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import uk.ac.ebi.biosamples.exceptions.GlobalExceptions;
import uk.ac.ebi.biosamples.model.*;
import uk.ac.ebi.biosamples.model.auth.AuthorizationProvider;
import uk.ac.ebi.biosamples.mongo.model.MongoFileUpload;
import uk.ac.ebi.biosamples.mongo.repo.MongoFileUploadRepository;
import uk.ac.ebi.biosamples.mongo.util.BioSamplesFileUploadSubmissionStatus;
import uk.ac.ebi.biosamples.mongo.util.SampleNameAccessionPair;
import uk.ac.ebi.biosamples.service.SampleService;
import uk.ac.ebi.biosamples.service.security.BioSamplesAapService;
import uk.ac.ebi.biosamples.service.security.BioSamplesWebinAuthenticationService;
import uk.ac.ebi.biosamples.utils.upload.FileUploadUtils;
import uk.ac.ebi.biosamples.utils.upload.ValidationResult;
import uk.ac.ebi.biosamples.validation.SchemaValidationService;

@Service
public class FileUploadService {
  private final Logger log = LoggerFactory.getLogger(getClass());
  private FileUploadUtils fileUploadUtils;

  @Autowired private SampleService sampleService;
  @Autowired private SchemaValidationService schemaValidationService;
  @Autowired private BioSamplesWebinAuthenticationService bioSamplesWebinAuthenticationService;
  @Autowired private BioSamplesAapService bioSamplesAapService;
  @Autowired private MongoFileUploadRepository mongoFileUploadRepository;
  @Autowired private FileQueueService fileQueueService;

  public synchronized File upload(
      final MultipartFile file,
      final String aapDomain,
      final String checklist,
      final String webinId,
      final FileUploadUtils fileUploadUtils) {
    final ValidationResult validationResult = new ValidationResult();

    this.fileUploadUtils = fileUploadUtils;
    final String authProvider = isWebin(isWebinIdUsedToAuthenticate(webinId));
    final boolean isWebin = authProvider.equals(FileUploadUtils.WEBIN_AUTH);
    final String uniqueUploadId = UUID.randomUUID().toString();

    try {
      final MongoFileUpload mongoFileUploadInitial =
          new MongoFileUpload(
              uniqueUploadId,
              BioSamplesFileUploadSubmissionStatus.ACTIVE,
              isWebin ? webinId : aapDomain,
              checklist,
              isWebin,
              new ArrayList<>(),
              null);

      mongoFileUploadRepository.insert(mongoFileUploadInitial);

      final Path temp = Files.createTempFile("upload", ".tsv");

      File fileToBeUploaded = temp.toFile();
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
            fileQueueService.queueFileinMongoAndSendMessageToRabbitMq(
                file, aapDomain, checklist, webinId);

        return fileUploadUtils.writeQueueMessageToFile(submissionId);
      }

      final List<Sample> samples =
          buildAndPersistSamples(
              csvDataMap, aapDomain, webinId, checklist, validationResult, isWebin);
      final List<SampleNameAccessionPair> accessionsList =
          samples.stream()
              .filter(sample -> sample.getAccession() != null)
              .map(sample -> new SampleNameAccessionPair(sample.getName(), sample.getAccession()))
              .collect(Collectors.toList());
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

      final MongoFileUpload mongoFileUploadCompleted =
          new MongoFileUpload(
              uniqueUploadId,
              bioSamplesFileUploadSubmissionStatus,
              isWebin ? webinId : aapDomain,
              checklist,
              isWebin,
              new ArrayList<>(),
              null);

      mongoFileUploadRepository.save(mongoFileUploadCompleted);

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

  private List<Sample> buildAndPersistSamples(
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
                buildAndPersistSample(
                    csvRecordMap, aapDomain, webinId, checklist, validationResult, isWebin);

            if (sample == null) {
              validationResult.addValidationMessage(
                  new ValidationResult.ValidationMessage(
                      fileUploadUtils.getSampleName(csvRecordMap),
                      "Failed to create sample in the file",
                      true));
            }
          } catch (Exception e) {
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
    Optional<Sample> oldSample = Optional.empty();

    final List<Relationship> relationships =
        fileUploadUtils.createRelationships(
            sample, sampleNameToAccessionMap, relationshipMap, validationResult);

    if (!sampleService.isNotExistingAccession(sample.getAccession())) {
      oldSample = sampleService.fetch(sample.getAccession(), Optional.empty());
    }

    if (relationships != null && relationships.size() > 0) {
      relationships.forEach(relationship -> log.info(relationship.toString()));

      sample = Sample.Builder.fromSample(sample).withRelationships(relationships).build();
      try {
        sample = storeSample(sample, oldSample, isWebin(isWebin));
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
      final String aapDomain,
      final String webinId,
      final String checklist,
      final ValidationResult validationResult,
      final boolean isWebin) {
    final String sampleName = fileUploadUtils.getSampleName(multiMap);
    boolean isValidatedAgainstChecklist;
    boolean sampleWithAccession = false;

    Sample sample = fileUploadUtils.buildSample(multiMap, validationResult);
    Optional<Sample> oldSample = Optional.empty();

    if (sample.getAccession() != null) {
      sampleWithAccession = true;

      if (!sampleService.isNotExistingAccession(sample.getAccession())) {
        oldSample = sampleService.fetch(sample.getAccession(), Optional.empty());
      }
    }

    if (oldSample.isPresent()) {
      log.info("Old sample is " + oldSample.get());
    } else {
      log.info("Old sample not present");
    }

    sample = handleAuthentication(aapDomain, webinId, isWebin, sample, oldSample, validationResult);

    if (sample != null) {
      sample = fileUploadUtils.addChecklistAttributeAndBuildSample(checklist, sample);

      isValidatedAgainstChecklist = performChecklistValidation(sample);

      if (isValidatedAgainstChecklist) {
        try {
          sample = storeSample(sample, oldSample, isWebin(isWebin));

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
      final String aapDomain,
      final String webinId,
      final boolean isWebin,
      Sample sample,
      final Optional<Sample> oldSample,
      final ValidationResult validationResult) {
    try {
      if (isWebin) {
        sample = Sample.Builder.fromSample(sample).withWebinSubmissionAccountId(webinId).build();
        sample =
            bioSamplesWebinAuthenticationService.handleWebinUserSubmission(
                sample, webinId, oldSample);
      } else {
        sample = Sample.Builder.fromSample(sample).withDomain(aapDomain).build();
        sample = bioSamplesAapService.handleSampleDomain(sample, oldSample);
      }

      return sample;
    } catch (final Exception e) {
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

  private String isWebin(final boolean isWebin) {
    return isWebin ? FileUploadUtils.WEBIN_AUTH : FileUploadUtils.AAP;
  }

  private Sample storeSample(
      final Sample sample, final Optional<Sample> oldSample, final String authProvider) {
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
      return sampleService.persistSample(
          sample, oldSample.orElse(null), AuthorizationProvider.valueOf(authProvider), false);
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
      final Pageable page = PageRequest.of(0, 10);
      return mongoFileUploadRepository.findBySubmitterDetailsIn(userRoles, page);
    } catch (final Exception e) {
      log.info("Failed in fetch submissions in getUserSubmissions() " + e.getMessage());
      throw new RuntimeException(e);
    }
  }
}
