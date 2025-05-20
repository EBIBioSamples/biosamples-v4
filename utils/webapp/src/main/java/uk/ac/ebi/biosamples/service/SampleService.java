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
package uk.ac.ebi.biosamples.service;

import static java.util.stream.Collectors.toSet;
import static uk.ac.ebi.biosamples.BioSamplesConstants.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.BioSamplesProperties;
import uk.ac.ebi.biosamples.exceptions.GlobalExceptions;
import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.Relationship;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.SubmittedViaType;
import uk.ac.ebi.biosamples.model.structured.AbstractData;
import uk.ac.ebi.biosamples.mongo.model.MongoRelationship;
import uk.ac.ebi.biosamples.mongo.model.MongoSample;
import uk.ac.ebi.biosamples.mongo.model.MongoSampleMessage;
import uk.ac.ebi.biosamples.mongo.repository.MongoSampleMessageRepository;
import uk.ac.ebi.biosamples.mongo.repository.MongoSampleRepository;
import uk.ac.ebi.biosamples.mongo.service.*;
import uk.ac.ebi.biosamples.service.security.BioSamplesCrossSourceIngestAccessControlService;

/**
 * Service layer business logic for centralising repository access and conversions between different
 * controller. Use this instead of linking to repositories directly.
 *
 * @author faulcon
 */
@Service
public class SampleService {
  private static final Logger log = LoggerFactory.getLogger(SampleService.class);
  private final MongoAccessionService mongoAccessionService;
  private final MongoSampleRepository mongoSampleRepository;
  private final MongoSampleMessageRepository mongoSampleMessageRepository;
  private final MongoSampleToSampleConverter mongoSampleToSampleConverter;
  private final SampleToMongoSampleConverter sampleToMongoSampleConverter;
  private final MongoRelationshipToRelationshipConverter mongoRelationshipToRelationshipConverter;
  private final SampleValidator sampleValidator;
  private final SampleReadService sampleReadService;
  private final MessagingService messagingService;
  private final BioSamplesProperties bioSamplesProperties;
  private final BioSamplesCrossSourceIngestAccessControlService
      bioSamplesCrossSourceIngestAccessControlService;

  @Autowired
  public SampleService(
      @Qualifier("SampleAccessionService") final MongoAccessionService mongoAccessionService,
      final MongoSampleRepository mongoSampleRepository,
      final MongoSampleMessageRepository mongoSampleMessageRepository,
      final MongoSampleToSampleConverter mongoSampleToSampleConverter,
      final SampleToMongoSampleConverter sampleToMongoSampleConverter,
      final MongoRelationshipToRelationshipConverter mongoRelationshipToRelationshipConverter,
      final SampleValidator sampleValidator,
      final SampleReadService sampleReadService,
      final MessagingService messagingService,
      final BioSamplesProperties bioSamplesProperties,
      final BioSamplesCrossSourceIngestAccessControlService
          bioSamplesCrossSourceIngestAccessControlService) {
    this.mongoAccessionService = mongoAccessionService;
    this.mongoSampleRepository = mongoSampleRepository;
    this.mongoSampleMessageRepository = mongoSampleMessageRepository;
    this.mongoSampleToSampleConverter = mongoSampleToSampleConverter;
    this.sampleToMongoSampleConverter = sampleToMongoSampleConverter;
    this.mongoRelationshipToRelationshipConverter = mongoRelationshipToRelationshipConverter;
    this.sampleValidator = sampleValidator;
    this.sampleReadService = sampleReadService;
    this.messagingService = messagingService;
    this.bioSamplesProperties = bioSamplesProperties;
    this.bioSamplesCrossSourceIngestAccessControlService =
        bioSamplesCrossSourceIngestAccessControlService;
  }

  /** Throws an IllegalArgumentException of no sample with that accession exists */
  public Optional<Sample> fetch(final String accession, final boolean applyCurations) {
    return sampleReadService.fetch(accession, applyCurations);
  }

  /*
  Checks if the current sample that exists has no metadata, returns true if empty
   */
  private boolean isStoredSampleEmpty(
      final Sample newSample, final boolean isWebinSuperUser, final Sample oldSample) {
    if (isWebinSuperUser) {
      if (newSample.getSubmittedVia() == SubmittedViaType.FILE_UPLOADER) {
        // file uploader submissions are done via superuser, but they are non-imported samples,
        // needs to be handled safely
        if (newSample.hasAccession()) {
          return isStoredSampleEmpty(oldSample);
        }

        return true;
      } else {
        // otherwise it is an ENA pipeline import, cannot be empty
        return false;
      }
    } else {
      if (newSample.hasAccession()) {
        return isStoredSampleEmpty(oldSample);
      }
    }

    if (newSample.hasAccession()) {
      return isStoredSampleEmpty(oldSample);
    }

    return true;
  }

  /*
  Checks if the current sample that exists has no metadata, returns true if empty
   */
  private boolean isStoredSampleEmpty(final Sample oldSample) {
    if (oldSample.getTaxId() != null && oldSample.getTaxId() > 0) {
      return false;
    }

    if (!oldSample.getAttributes().isEmpty()) {
      return false;
    }

    if (!oldSample.getRelationships().isEmpty()) {
      return false;
    }

    if (!oldSample.getPublications().isEmpty()) {
      return false;
    }

    if (!oldSample.getContacts().isEmpty()) {
      return false;
    }

    if (!oldSample.getOrganizations().isEmpty()) {
      return false;
    }

    if (!oldSample.getData().isEmpty()) {
      return false;
    }

    if (!oldSample.getExternalReferences().isEmpty()) {
      return false;
    }

    return oldSample.getStructuredData().isEmpty();
  }

  // Because the fetch caches the sample, if an updated version is stored, we need to make
  // sure
  // that any cached version
  // is removed.
  // Note, pages of samples will not be cache busted, only single-accession sample retrieval
  // @CacheEvict(cacheNames=WebappProperties.fetchUsing, key="#result.accession")
  /*
  Called by V1 endpoints to persist samples
   */
  public Sample persistSample(
      Sample newSample, final Sample oldSample, final boolean isWebinSuperUser) {
    final Collection<String> errors = sampleValidator.validate(newSample);

    if (!errors.isEmpty()) {
      log.error("Sample validation failed : {}", errors);

      throw new GlobalExceptions.SampleMandatoryFieldsMissingException(String.join("|", errors));
    }

    if (newSample.hasAccession()) {
      if (oldSample != null) {
        newSample = updateFromCurrent(newSample, oldSample, isWebinSuperUser);
      } else {
        newSample = updateWhenNoneExists(newSample);
      }

      MongoSample mongoSample = sampleToMongoSampleConverter.convert(newSample);
      mongoSample = mongoSampleRepository.save(mongoSample);

      if (isTaxIdUpdated(oldSample, newSample)) {
        mongoSampleMessageRepository.save(
            new MongoSampleMessage(newSample.getAccession(), Instant.now(), newSample.getTaxId()));
      }

      newSample = mongoSampleToSampleConverter.apply(mongoSample);
      sendMessageToRabbitForIndexingToSolr(
          newSample.getAccession(), getExistingRelationshipTargetsForIndexingInSolr(oldSample));
    } else {
      newSample = createNew(newSample);
    }

    // fetch returns sample with curations applied
    final Optional<Sample> sampleOptional = fetch(newSample.getAccession(), true);

    return sampleOptional.orElseThrow(
        () ->
            new RuntimeException(
                "Failed to create sample. Please contact the BioSamples Helpdesk at biosamples@ebi.ac.uk"));
  }

  private Sample updateWhenNoneExists(Sample newSample) {
    log.error("Trying to update sample not in database, accession: {}", newSample.getAccession());

    bioSamplesCrossSourceIngestAccessControlService
        .validateFileUploaderSampleUpdateHasExistingAccession(newSample);

    final SortedSet<Attribute> newSampleAttributes = newSample.getAttributes();

    if (newSampleAttributes.stream()
        .noneMatch(attribute -> attribute.getType().equals(SRA_ACCESSION))) {
      // Don't generate SRA accession (ERS sample accessions) for NCBI samples
      if (!isNcbiImport(newSample)) {
        final String sraAccession = generateOneSRAAccession();

        newSampleAttributes.add(Attribute.build(SRA_ACCESSION, sraAccession));
        newSample = Sample.Builder.fromSample(newSample).withSraAccession(sraAccession).build();
      }
    } else {
      final List<Attribute> sraAccessionAttributeList =
          newSampleAttributes.stream()
              .filter(attribute -> attribute.getType().equals(SRA_ACCESSION))
              .toList();

      if (sraAccessionAttributeList.size() > 1) {
        throw new GlobalExceptions.InvalidSampleException();
      }

      final String sraAccession = sraAccessionAttributeList.get(0).getValue();

      newSample = Sample.Builder.fromSample(newSample).withSraAccession(sraAccession).build();
    }

    return newSample;
  }

  private Sample updateFromCurrent(
      Sample newSample, final Sample oldSample, final boolean isWebinSuperUser) {
    final boolean savedSampleEmpty = isStoredSampleEmpty(newSample, isWebinSuperUser, oldSample);

    if (savedSampleEmpty) {
      newSample = Sample.Builder.fromSample(newSample).withSubmitted(Instant.now()).build();
    }

    final List<Relationship> existingRelationships =
        getExistingRelationshipTargetsForIndexingInSolr(
            newSample.getAccession(),
            Objects.requireNonNull(sampleToMongoSampleConverter.convert(oldSample)));

    return compareWithExistingAndUpdateSample(
        newSample, oldSample, existingRelationships, savedSampleEmpty, isWebinSuperUser);
  }

  private Sample createNew(Sample newSample) {
    final boolean noSraAccession =
        newSample.getAttributes().stream()
            .noneMatch(attribute -> attribute.getType().equals(SRA_ACCESSION));

    if (!noSraAccession) {
      newSample = validateAndPromoteSRAAccessionAttributeToField(newSample);
    }

    newSample = mongoAccessionService.generateAccession(newSample, noSraAccession);
    sendMessageToRabbitForIndexingToSolr(newSample.getAccession(), Collections.emptyList());

    return newSample;
  }

  private boolean isTaxIdUpdated(final Sample oldSample, final Sample sample) {
    return oldSample != null
        && oldSample.getTaxId() != null
        && !oldSample.getTaxId().equals(sample.getTaxId());
  }

  private List<String> getExistingRelationshipTargetsForIndexingInSolr(final Sample oldSample) {
    final List<String> existingRelationshipTargets = new ArrayList<>();

    if (oldSample != null) {
      final List<Relationship> existingRelationships =
          getExistingRelationshipTargetsForIndexingInSolr(
              oldSample.getAccession(),
              Objects.requireNonNull(sampleToMongoSampleConverter.convert(oldSample)));

      existingRelationshipTargets.addAll(
          existingRelationships.stream()
              .map(
                  relationship -> {
                    if (relationship.getSource().equals(oldSample.getAccession())) {
                      return relationship.getTarget();
                    }

                    return null;
                  })
              .toList());
    }

    return existingRelationshipTargets;
  }

  /*
  Called by V2 endpoints to persist samples
   */
  public Sample persistSampleV2(
      Sample newSample, final Sample oldSample, final boolean isWebinSuperUser) {
    final Collection<String> errors = sampleValidator.validate(newSample);

    if (!errors.isEmpty()) {
      log.error("Sample validation failed : {}", errors);
      throw new GlobalExceptions.SampleMandatoryFieldsMissingException(String.join("|", errors));
    }

    if (newSample.hasAccession()) {
      if (oldSample != null) {
        log.info(
            "Trying to update sample that exists in database, accession: {}",
            newSample.getAccession());

        final boolean savedSampleEmpty =
            isStoredSampleEmpty(newSample, isWebinSuperUser, oldSample);

        if (savedSampleEmpty) {
          newSample = Sample.Builder.fromSample(newSample).withSubmitted(Instant.now()).build();
        }

        newSample =
            compareWithExistingAndUpdateSample(
                newSample, oldSample, null, savedSampleEmpty, isWebinSuperUser);
      } else {
        log.error(
            "Trying to update sample not in database, accession: {}", newSample.getAccession());

        newSample = updateWhenNoneExists(newSample);
      }

      MongoSample mongoSample = sampleToMongoSampleConverter.convert(newSample);

      assert mongoSample != null;

      mongoSample = mongoSampleRepository.save(mongoSample);
      newSample = mongoSampleToSampleConverter.apply(mongoSample);

      sendMessageToRabbitForIndexingToSolr(newSample.getAccession(), Collections.emptyList());
    } else {
      newSample = createNew(newSample);
    }

    return newSample;
  }

  private void sendMessageToRabbitForIndexingToSolr(
      final String accession, final List<String> existingRelationshipTargets) {
    try {
      messagingService.fetchThenSendMessage(accession, existingRelationshipTargets);
    } catch (final Exception e) {
      log.error("Indexing failed for accession " + accession);
    }
  }

  /*
  Called by V2 endpoints to build a sample with a newly generated sample accession
   */
  public Sample accessionSample(Sample newSample) {
    final Collection<String> errors = sampleValidator.validate(newSample);

    if (!errors.isEmpty()) {
      log.error("Sample validation failed : {}", errors);

      throw new GlobalExceptions.SampleMandatoryFieldsMissingException(String.join("|", errors));
    }

    if (newSample
        .getWebinSubmissionAccountId()
        .equalsIgnoreCase(bioSamplesProperties.getBiosamplesClientWebinUsername())) {
      // accessioning from ENA, sample name is the SRA accession here
      final Attribute sraAccessionAttribute = Attribute.build(SRA_ACCESSION, newSample.getName());

      newSample.getAttributes().add(sraAccessionAttribute);
      newSample = Sample.Builder.fromSample(newSample).build();

      return mongoAccessionService.generateAccession(newSample, false);
    } else {
      return mongoAccessionService.generateAccession(newSample, true);
    }
  }

  public String generateOneSRAAccession() {
    return mongoAccessionService.generateOneSRAAccession();
  }

  /*
  Returns true if a sample does not exist in BioSamples
   */
  public boolean isNotExistingAccession(final String accession) {
    if (accession != null) {
      return mongoSampleRepository.findById(accession).isEmpty();
    } else {
      return true;
    }
  }

  private List<Relationship> getExistingRelationshipTargetsForIndexingInSolr(
      final String accession, final MongoSample mongoOldSample) {
    final List<Relationship> oldRelationshipTargets = new ArrayList<>();

    for (final MongoRelationship mongoRelationship : mongoOldSample.getRelationships()) {
      if (mongoRelationship.getSource().equals(accession)) {
        oldRelationshipTargets.add(
            mongoRelationshipToRelationshipConverter.convert(mongoRelationship));
      }
    }

    return oldRelationshipTargets;
  }

  private Sample compareWithExistingAndUpdateSample(
      Sample newSample,
      final Sample oldSample,
      final List<Relationship> existingRelationships,
      final boolean isEmptySample,
      final boolean isWebinSuperUser) {
    Set<AbstractData> structuredData = new HashSet<>();
    boolean applyOldSampleStructuredData = false;

    // retain existing relationships for superuser submissions, pipelines, ENA POSTED, not for file
    // uploads though
    handleRelationships(newSample, existingRelationships);
    handleSRAAccession(newSample, oldSample, isWebinSuperUser);
    newSample = validateAndPromoteSRAAccessionAttributeToField(newSample);

    if (newSample.getData().isEmpty()) {
      log.info("No structured data in new sample");

      if (oldSample.getData() != null && !oldSample.getData().isEmpty()) {
        structuredData = oldSample.getData();
        // Check if old sample has structured data, if yes, retain
        applyOldSampleStructuredData = true;

        log.info("Old sample has structured data");
      }
    }

    if (applyOldSampleStructuredData) {
      log.info("Build sample and applying old sample structured data");
      log.trace("Old sample structured data size is " + structuredData.size());

      return Sample.Builder.fromSample(newSample)
          .withCreate(defineCreateDate(newSample, oldSample))
          .withSubmitted(defineSubmittedDate(newSample, oldSample, isEmptySample))
          .withData(structuredData)
          .build();
    } else {
      log.info("Building sample without structured data");

      return Sample.Builder.fromSample(newSample)
          .withCreate(defineCreateDate(newSample, oldSample))
          .withSubmitted(defineSubmittedDate(newSample, oldSample, isEmptySample))
          .build();
    }
  }

  private Sample validateAndPromoteSRAAccessionAttributeToField(final Sample newSample) {
    // Retrieve SRA accession attribute from new sample
    final Optional<Attribute> newSampleSraAccessionOptional =
        newSample.getAttributes().stream()
            .filter(attribute -> attribute.getType().equalsIgnoreCase(SRA_ACCESSION))
            .findFirst();

    // Retrieve SRA accession field from new sample
    final String sraAccessionField = newSample.getSraAccession();

    // Check if SRA accession field and attribute are both present
    if (sraAccessionField != null && newSampleSraAccessionOptional.isPresent()) {
      // Check for SRA accession mismatch
      if (!sraAccessionField.equals(newSampleSraAccessionOptional.get().getValue())) {
        throw new GlobalExceptions.InvalidSampleException();
      } else {
        // If they match, return the new sample
        return newSample;
      }
    }

    // Check if SRA accession field is null but the attribute is present
    if (sraAccessionField == null && newSampleSraAccessionOptional.isPresent()) {
      // Promote SRA accession attribute to the field and return the modified sample
      return Sample.Builder.fromSample(newSample)
          .withSraAccession(newSampleSraAccessionOptional.get().getValue())
          .build();
    }

    // Return the original new sample if no promotion or validation is needed
    return newSample;
  }

  private void handleRelationships(
      final Sample newSample, final List<Relationship> existingRelationships) {
    if (existingRelationships != null && !existingRelationships.isEmpty()) {
      final String webinId = newSample.getWebinSubmissionAccountId();

      // superuser and non-file upload submissions
      if (webinId != null
          && webinId.equals(bioSamplesProperties.getBiosamplesClientWebinUsername())) {
        if (newSample.getSubmittedVia() != SubmittedViaType.FILE_UPLOADER) {
          newSample.getRelationships().addAll(existingRelationships);
        }
      }
    }
  }

  private void handleSRAAccession(
      final Sample newSample, final Sample oldSample, final boolean isWebinSuperUser) {
    // Step 1: Initialization
    final String newSampleSraAccessionField = newSample.getSraAccession();

    // Step 3: Validation of SRA Accession Field in New Sample
    if (newSampleSraAccessionField != null && !newSampleSraAccessionField.isEmpty()) {
      if (oldSample != null) {
        // Check if the SRA accession field has changed in the new sample
        final String oldSampleSraAccessionField = oldSample.getSraAccession();

        /*
         * <p>This logic performs the following checks:
         * <ul>
         *   <li>If the old SRA accession field is not null and not empty.</li>
         *   <li>If the new SRA accession field is different from the old SRA accession field.</li>
         *   <li>If the user is not a Webin Super User.</li>
         *   <li>If the sample is not an NCBI sample in an NCBI Super User domain.</li>
         * </ul>
         */
        if (oldSampleSraAccessionField != null
            && !oldSampleSraAccessionField.isEmpty()
            && !newSampleSraAccessionField.equals(oldSampleSraAccessionField)
            && !isWebinSuperUser
            && !isNcbiImport(newSample)) {
          throw new GlobalExceptions.ChangedSRAAccessionException();
        }
      }
    }

    final SortedSet<Attribute> newSampleAttributes = newSample.getAttributes();
    final List<Attribute> newSampleSraAccessionAttributes =
        newSampleAttributes.stream()
            .filter(attribute -> attribute.getType().equalsIgnoreCase(SRA_ACCESSION))
            .toList();

    // Step 2: Validation of SRA Accession attributes in New Sample
    if (newSampleSraAccessionAttributes.size() > 1) {
      throw new GlobalExceptions.InvalidSampleException();
    }

    Attribute newSampleSraAccessionAttribute =
        newSampleSraAccessionAttributes.isEmpty() ? null : newSampleSraAccessionAttributes.get(0);

    // Step 4: Validation of SRA Accession attributes in Old Sample
    if (oldSample != null) {
      final List<Attribute> oldSampleSraAccessionAttributes =
          oldSample.getAttributes().stream()
              .filter(attribute -> attribute.getType().equalsIgnoreCase(SRA_ACCESSION))
              .toList();

      // Check if there are more than one SRA accession attributes in the old sample
      if (oldSampleSraAccessionAttributes.size() > 1) {
        throw new GlobalExceptions.InvalidSampleException();
      }

      final Attribute oldSampleSraAccessionAttribute =
          oldSampleSraAccessionAttributes.isEmpty() ? null : oldSampleSraAccessionAttributes.get(0);

      // Step 5: Handling SRA Accessions in New Sample and Old Sample
      if (newSampleSraAccessionAttribute == null) {
        // If newSampleSraAccessionAttribute is null, use oldSampleSraAccession or generate a new
        // one
        newSampleSraAccessionAttribute =
            Objects.requireNonNullElseGet(
                oldSampleSraAccessionAttribute,
                () -> Attribute.build(SRA_ACCESSION, generateOneSRAAccession()));
        // Add newSampleSraAccessionAttribute to the attributes of the new sample
        newSampleAttributes.add(newSampleSraAccessionAttribute);
      }

      // Step 6: Validation of Changed SRA Accession (if Old Sample exists)
      if (oldSampleSraAccessionAttribute != null
          && !oldSampleSraAccessionAttribute
              .getValue()
              .equals(newSampleSraAccessionAttribute.getValue())
          && !isWebinSuperUser
          && !isNcbiImport(newSample)) {
        throw new GlobalExceptions.ChangedSRAAccessionException();
      }
    } else {
      // Step 7: Handling New Samples without Old Samples (Old Sample doesn't exist)
      if (newSampleSraAccessionAttribute == null && isWebinSuperUser && !isNcbiImport(newSample)) {
        // If oldSample doesn't exist, and newSampleSraAccessionAttribute is still null, create, a
        // new one
        // Doesn't generate SRA accession (ERS sample accessions) for NCBI samples while import
        // (prefix checks)
        newSampleSraAccessionAttribute = Attribute.build(SRA_ACCESSION, generateOneSRAAccession());
        // Add newSampleSraAccessionAttribute to the attributes of the new sample
        newSampleAttributes.add(newSampleSraAccessionAttribute);
      }
    }
  }

  private boolean isNcbiImport(final Sample newSample) {
    return (newSample.getAccession().startsWith(NCBI_ACCESSION_PREFIX)
            || newSample.getAccession().startsWith(DDBJ_ACCESSION_PREFIX))
        && newSample.getSubmittedVia() == SubmittedViaType.PIPELINE_IMPORT;
  }

  private Instant defineCreateDate(final Sample newSample, final Sample oldSample) {
    return (oldSample.getCreate() != null ? oldSample.getCreate() : newSample.getCreate());
  }

  public boolean isPipelineEnaDomain(final String domain) {
    if (domain == null) {
      return false;
    }

    return domain.equalsIgnoreCase(ENA_IMPORT_DOMAIN);
  }

  public boolean isPipelineNcbiDomain(final String domain) {
    if (domain == null) {
      return false;
    }

    return domain.equalsIgnoreCase(NCBI_IMPORT_DOMAIN);
  }

  private Instant defineSubmittedDate(
      final Sample newSample, final Sample oldSample, final boolean isEmptySample) {
    if (isEmptySample) {
      return newSample.getSubmitted();
    } else {
      return oldSample.getSubmitted() != null ? oldSample.getSubmitted() : newSample.getSubmitted();
    }
  }

  public Instant defineCreateDate(final Sample sample, final boolean isWebinSuperUserSubmission) {
    final Instant now = Instant.now();
    final Instant create = sample.getCreate();

    return isWebinSuperUserSubmission ? (create != null ? create : now) : now;
  }

  public Instant defineSubmittedDate(
      final Sample sample, final boolean isWebinSuperUserSubmission) {
    final Instant now = Instant.now();
    final Instant submitted = sample.getSubmitted();

    return isWebinSuperUserSubmission ? (submitted != null ? submitted : now) : now;
  }

  public Sample buildPrivateSample(final Sample sample) {
    final Instant release =
        Instant.ofEpochSecond(
            LocalDateTime.now(ZoneOffset.UTC).plusYears(100).toEpochSecond(ZoneOffset.UTC));
    final Instant update = Instant.now();
    final SubmittedViaType submittedVia =
        sample.getSubmittedVia() == null ? SubmittedViaType.JSON_API : sample.getSubmittedVia();

    return Sample.Builder.fromSample(sample)
        .withRelease(release)
        .withUpdate(update)
        .withSubmittedVia(submittedVia)
        .build();
  }

  public Set<Relationship> handleSampleRelationshipsV2(
      final Sample sample,
      final Optional<Sample> oldSampleOptional,
      final boolean isSuperUserSubmission) {
    final SortedSet<Relationship> sampleRelationships = sample.getRelationships();

    if (!isSuperUserSubmission) {
      if (sampleRelationships != null && !sampleRelationships.isEmpty()) {
        throw new GlobalExceptions.SampleWithRelationshipSubmissionExceptionV2();
      }
    }

    if (sample.hasAccession()) {
      if (oldSampleOptional.isPresent()) {
        final Sample oldSample = oldSampleOptional.get();

        if (oldSample.getRelationships() != null && !oldSample.getRelationships().isEmpty()) {
          return Stream.of(oldSample.getRelationships(), sampleRelationships)
              .filter(Objects::nonNull)
              .flatMap(Set::stream)
              .collect(toSet());
        }
      } else {
        return sampleRelationships;
      }
    } else {
      if (sampleRelationships != null && !sampleRelationships.isEmpty()) {
        throw new GlobalExceptions.SampleWithRelationshipSubmissionExceptionV2();
      }
    }

    return null;
  }

  public Optional<Sample> validateSampleWithAccessionsAgainstConditionsAndGetOldSample(
      final Sample sample, final boolean isWebinSuperUser) {
    if (!isWebinSuperUser) {
      if (sample.hasAccession()
          || sample.hasSraAccession()
          || sample.getAttributes() != null
              && sample.getAttributes().stream()
                  .anyMatch(attribute -> attribute.getType().equalsIgnoreCase(SRA_ACCESSION))) {
        throw new GlobalExceptions.SampleWithAccessionSubmissionException();
      }
    } else {
      final List<Attribute> sraAccessionAttributeList =
          sample.getAttributes().stream()
              .filter(attribute -> attribute.getType().equalsIgnoreCase(SRA_ACCESSION))
              .toList();
      if (sraAccessionAttributeList.size() > 1) {
        throw new GlobalExceptions.InvalidSampleException();
      } else {
        String sraAccession = null;

        if (!sraAccessionAttributeList.isEmpty()) {
          sraAccession = sraAccessionAttributeList.get(0).getValue();
        }

        if (sraAccession != null
            && sample.getSraAccession() != null
            && !Objects.equals(sraAccession, sample.getSraAccession())) {
          throw new GlobalExceptions.InvalidSampleException();
        }
      }

      if (sample.hasAccession() && !isNotExistingAccession(sample.getAccession())) {
        // fetch old sample if sample exists,
        // fetch returns sample with curations applied
        return fetch(sample.getAccession(), false);
      }
    }

    return Optional.empty();
  }

  public String getPrinciple(final Authentication loggedInUser) {
    if (loggedInUser == null || loggedInUser.getPrincipal() == null) {
      log.warn("Unauthenticated access detected: returning null for principal");
      return null;
    }

    return loggedInUser.getPrincipal() instanceof UserDetails
        ? ((UserDetails) loggedInUser.getPrincipal()).getUsername()
        : loggedInUser.getPrincipal().toString();
  }
}
