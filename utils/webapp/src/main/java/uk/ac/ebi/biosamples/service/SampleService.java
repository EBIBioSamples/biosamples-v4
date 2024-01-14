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
import static uk.ac.ebi.biosamples.utils.BioSamplesConstants.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.BioSamplesProperties;
import uk.ac.ebi.biosamples.exceptions.GlobalExceptions;
import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.Relationship;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.SubmittedViaType;
import uk.ac.ebi.biosamples.model.auth.AuthorizationProvider;
import uk.ac.ebi.biosamples.model.structured.AbstractData;
import uk.ac.ebi.biosamples.mongo.model.MongoAccessionMapping;
import uk.ac.ebi.biosamples.mongo.model.MongoRelationship;
import uk.ac.ebi.biosamples.mongo.model.MongoSample;
import uk.ac.ebi.biosamples.mongo.model.MongoSampleMessage;
import uk.ac.ebi.biosamples.mongo.repo.MongoAccessionMappingRepository;
import uk.ac.ebi.biosamples.mongo.repo.MongoSampleMessageRepository;
import uk.ac.ebi.biosamples.mongo.repo.MongoSampleRepository;
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
  private final MongoAccessionMappingRepository mongoAccessionMappingRepository;
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
      final MongoAccessionMappingRepository mongoAccessionMappingRepository,
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
    this.mongoAccessionMappingRepository = mongoAccessionMappingRepository;
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
  public Optional<Sample> fetch(
      final String accession, final Optional<List<String>> curationDomains) {
    return sampleReadService.fetch(accession, curationDomains);
  }

  /*
  Checks if the current newSample that exists has no metadata, returns true if empty
   */
  private boolean isSavedSampleEmpty(
      final Sample newSample, final boolean isWebinSuperUser, final Sample oldSample) {
    final String domain = newSample.getDomain();

    if (isWebinSuperUser) {
      if (newSample.getSubmittedVia() == SubmittedViaType.FILE_UPLOADER) {
        // file uploader submissions are done via super-user, but they are non imported samples,
        // needs to be handled safely
        if (newSample.hasAccession()) {
          return isSavedSampleEmpty(oldSample);
        }

        return true;
      } else {
        // otherwise it is a ENA pipeline import, cannot be empty
        return false;
      }
    } else {
      if (newSample.hasAccession()) {
        return isSavedSampleEmpty(oldSample);
      }
    }

    if (isAPipelineAapDomain(domain)) {
      return false; // imported newSample - never submitted first time to BSD, always has metadata
    } else {
      if (newSample.hasAccession()) {
        return isSavedSampleEmpty(oldSample);
      }

      return true;
    }
  }

  public boolean isAPipelineAapDomain(final String domain) {
    return isPipelineEnaDomain(domain) || isPipelineNcbiDomain(domain);
  }

  /*
  Checks if the current sample that exists has no metadata, returns true if empty
   */
  private boolean isSavedSampleEmpty(final Sample oldSample) {
    if (oldSample.getTaxId() != null && oldSample.getTaxId() > 0) {
      return false;
    }

    if (oldSample.getAttributes().size() > 0) {
      return false;
    }

    if (oldSample.getRelationships().size() > 0) {
      return false;
    }

    if (oldSample.getPublications().size() > 0) {
      return false;
    }

    if (oldSample.getContacts().size() > 0) {
      return false;
    }

    if (oldSample.getOrganizations().size() > 0) {
      return false;
    }

    if (oldSample.getData().size() > 0) {
      return false;
    }

    if (oldSample.getExternalReferences().size() > 0) {
      return false;
    }

    return oldSample.getStructuredData().size() == 0;
  }

  // because the fetchUsing caches the newSample, if an updated version is stored, we need to make
  // sure
  // that any cached version
  // is removed.
  // Note, pages of samples will not be cache busted, only single-accession newSample retrieval
  // @CacheEvict(cacheNames=WebappProperties.fetchUsing, key="#result.accession")
  /*
  Called by V1 endpoints to persist samples
   */
  public Sample persistSample(
      Sample newSample,
      final Sample oldSample,
      final AuthorizationProvider authProvider,
      final boolean isWebinSuperUser) {
    validateSample(newSample);

    if (newSample.hasAccession()) {
      if (oldSample != null) {
        newSample = updateSampleWithExisting(newSample, oldSample, authProvider, isWebinSuperUser);
      } else {
        handleSampleNotInDatabase(newSample);
      }

      MongoSample mongoSample = sampleToMongoSampleConverter.convert(newSample);
      mongoSample = mongoSampleRepository.save(mongoSample);

      if (isSampleTaxIdUpdated(oldSample, newSample)) {
        mongoSampleMessageRepository.save(
            new MongoSampleMessage(newSample.getAccession(), Instant.now(), newSample.getTaxId()));
      }

      newSample = mongoSampleToSampleConverter.apply(mongoSample);

      registerAccessionToSraAccessionMapping(newSample);
      sendMessageToRabbitForIndexingToSolr(
          newSample.getAccession(), getExistingRelationshipTargetsForindesinginSolr(oldSample));

    } else {
      newSample = handleSampleNotAccessioned(newSample);
    }

    final Optional<Sample> sampleOptional = fetch(newSample.getAccession(), Optional.empty());

    return sampleOptional.orElseThrow(
        () ->
            new RuntimeException(
                "Failed to create newSample. Please contact the BioSamples Helpdesk at biosamples@ebi.ac.uk"));
  }

  private void validateSample(final Sample sample) {
    final Collection<String> errors = sampleValidator.validate(sample);

    if (!errors.isEmpty()) {
      log.error("Sample validation failed : {}", errors);

      throw new GlobalExceptions.SampleValidationControllerException(String.join("|", errors));
    }
  }

  private Sample updateSampleWithExisting(
      Sample newSample,
      final Sample oldSample,
      final AuthorizationProvider authProvider,
      final boolean isWebinSuperUser) {
    final boolean savedSampleEmpty = isSavedSampleEmpty(newSample, isWebinSuperUser, oldSample);

    if (savedSampleEmpty) {
      newSample = Sample.Builder.fromSample(newSample).withSubmitted(Instant.now()).build();
    }

    final List<Relationship> existingRelationships =
        getExistingRelationshipTargetsForindesinginSolr(
            newSample.getAccession(),
            Objects.requireNonNull(sampleToMongoSampleConverter.convert(oldSample)));

    return compareWithExistingAndUpdateSample(
        newSample,
        oldSample,
        existingRelationships,
        savedSampleEmpty,
        authProvider,
        isWebinSuperUser);
  }

  private void handleSampleNotInDatabase(final Sample sample) {
    log.error("Trying to update sample not in database, accession: {}", sample.getAccession());

    bioSamplesCrossSourceIngestAccessControlService
        .validateFileUploaderSampleUpdateHasAlwaysExistingAccession(sample);
  }

  private Sample handleSampleNotAccessioned(Sample sample) {
    final boolean sampleNotSraAccessioned =
        sample.getAttributes().stream()
            .noneMatch(attribute -> attribute.getType().equals(SRA_ACCESSION));

    sample = mongoAccessionService.generateAccession(sample, sampleNotSraAccessioned);
    registerAccessionToSraAccessionMapping(sample);
    sendMessageToRabbitForIndexingToSolr(sample.getAccession(), Collections.emptyList());

    return sample;
  }

  private boolean isSampleTaxIdUpdated(final Sample oldSample, final Sample sample) {
    return oldSample != null
        && oldSample.getTaxId() != null
        && !oldSample.getTaxId().equals(sample.getTaxId());
  }

  private List<String> getExistingRelationshipTargetsForindesinginSolr(final Sample oldSample) {
    final List<String> existingRelationshipTargets = new ArrayList<>();

    if (oldSample != null) {
      final List<Relationship> existingRelationships =
          getExistingRelationshipTargetsForindesinginSolr(
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

  private void registerAccessionToSraAccessionMapping(final Sample sample) {
    final Optional<Attribute> sraAccessionAttributeOptional =
        sample.getAttributes().stream()
            .filter(attribute -> attribute.getType().equals(SRA_ACCESSION))
            .findFirst();

    sraAccessionAttributeOptional.ifPresent(
        attribute ->
            mongoAccessionMappingRepository.save(
                new MongoAccessionMapping(sample.getAccession(), attribute.getValue())));
  }

  /*
  Called by V2 endpoints to persist samples
   */
  public Sample persistSampleV2(
      Sample newSample,
      final Sample oldSample,
      final AuthorizationProvider authProvider,
      final boolean isWebinSuperUser) {
    final Collection<String> errors = sampleValidator.validate(newSample);

    if (!errors.isEmpty()) {
      log.error("Sample validation failed : {}", errors);
      throw new GlobalExceptions.SampleValidationControllerException(String.join("|", errors));
    }

    if (newSample.hasAccession()) {
      if (oldSample != null) {
        log.info(
            "Trying to update newSample that exists in database, accession: {}",
            newSample.getAccession());

        final boolean savedSampleEmpty = isSavedSampleEmpty(newSample, isWebinSuperUser, oldSample);

        if (savedSampleEmpty) {
          newSample = Sample.Builder.fromSample(newSample).withSubmitted(Instant.now()).build();
        }

        newSample =
            compareWithExistingAndUpdateSample(
                newSample, oldSample, null, savedSampleEmpty, authProvider, isWebinSuperUser);
      } else {
        log.error(
            "Trying to update newSample not in database, accession: {}", newSample.getAccession());
      }

      MongoSample mongoSample = sampleToMongoSampleConverter.convert(newSample);

      assert mongoSample != null;

      mongoSample = mongoSampleRepository.save(mongoSample);
      newSample = mongoSampleToSampleConverter.apply(mongoSample);

    } else {
      final boolean sampleNotSraAccessioned =
          newSample.getAttributes().stream()
              .noneMatch(attribute -> attribute.getType().equals(SRA_ACCESSION));

      newSample = mongoAccessionService.generateAccession(newSample, sampleNotSraAccessioned);
    }

    registerAccessionToSraAccessionMapping(newSample);
    sendMessageToRabbitForIndexingToSolr(newSample.getAccession(), Collections.emptyList());

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
  Called by V2 endpoints to build a newSample with a newly generated newSample accession
   */
  public Sample accessionSample(Sample newSample) {
    final Collection<String> errors = sampleValidator.validate(newSample);

    if (!errors.isEmpty()) {
      log.error("Sample validation failed : {}", errors);
      throw new GlobalExceptions.SampleValidationControllerException(String.join("|", errors));
    }

    if (newSample
        .getWebinSubmissionAccountId()
        .equalsIgnoreCase(bioSamplesProperties.getBiosamplesClientWebinUsername())) {
      // accessioning from ENA, newSample name is the SRA accession here
      final Attribute sraAccessionAttribute = Attribute.build(SRA_ACCESSION, newSample.getName());

      newSample.getAttributes().add(sraAccessionAttribute);
      newSample = Sample.Builder.fromSample(newSample).build();

      final Sample accessionedSample = mongoAccessionService.generateAccession(newSample, false);

      registerAccessionToSraAccessionMapping(newSample);
      return accessionedSample;
    } else {
      final Sample accessionedSample = mongoAccessionService.generateAccession(newSample, true);

      registerAccessionToSraAccessionMapping(newSample);
      return accessionedSample;
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

  private List<Relationship> getExistingRelationshipTargetsForindesinginSolr(
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
      final Sample newSample,
      final Sample oldSample,
      final List<Relationship> existingRelationships,
      final boolean isEmptySample,
      final AuthorizationProvider authProvider,
      final boolean isWebinSuperUser) {
    Set<AbstractData> structuredData = new HashSet<>();
    boolean applyOldSampleStructuredData = false;

    // retain existing relationships for super user submissions, pipelines, ENA POSTED, not for file
    // uploads though
    handleRelationships(newSample, existingRelationships);
    handleSRAAccession(newSample, oldSample, isWebinSuperUser);

    if (newSample.getData().size() < 1) {
      log.info("No structured data in new sample");

      if (oldSample.getData() != null && oldSample.getData().size() > 0) {
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
          .withCreate(defineCreateDate(newSample, oldSample, authProvider))
          .withSubmitted(defineSubmittedDate(newSample, oldSample, isEmptySample, authProvider))
          .withData(structuredData)
          .build();
    } else {
      log.info("Building sample without structured data");

      return Sample.Builder.fromSample(newSample)
          .withCreate(defineCreateDate(newSample, oldSample, authProvider))
          .withSubmitted(defineSubmittedDate(newSample, oldSample, isEmptySample, authProvider))
          .build();
    }
  }

  private void handleRelationships(
      final Sample newSample, final List<Relationship> existingRelationships) {
    if (existingRelationships != null && existingRelationships.size() > 0) {
      final String webinId = newSample.getWebinSubmissionAccountId();
      final String domain = newSample.getDomain();

      // superuser and non file upload submissions
      if ((webinId != null
              && webinId.equals(bioSamplesProperties.getBiosamplesClientWebinUsername()))
          || domain != null && domain.equals(bioSamplesProperties.getBiosamplesAapSuperWrite())) {
        if (newSample.getSubmittedVia() != SubmittedViaType.FILE_UPLOADER) {
          newSample.getRelationships().addAll(existingRelationships);
        }
      }
    }
  }

  private void handleSRAAccession(
      final Sample newSample, final Sample oldSample, final boolean isWebinSuperUser) {
    final SortedSet<Attribute> newSampleAttributes = newSample.getAttributes();
    final List<Attribute> newSampleSraAccessions =
        newSampleAttributes.stream()
            .filter(attribute -> attribute.getType().equalsIgnoreCase(SRA_ACCESSION))
            .toList();

    if (newSampleSraAccessions.size() > 1) {
      throw new GlobalExceptions.InvalidSampleException();
    }

    Attribute newSampleSraAccession = null;

    if (newSampleSraAccessions.size() > 0) {
      newSampleSraAccession = newSampleSraAccessions.get(0);
    }

    if (oldSample != null) {
      final List<Attribute> oldSampleSraAccessions =
          oldSample.getAttributes().stream()
              .filter(attribute -> attribute.getType().equalsIgnoreCase(SRA_ACCESSION))
              .toList();

      if (oldSampleSraAccessions.size() > 1) {
        throw new GlobalExceptions.InvalidSampleException();
      }

      Attribute oldSampleSraAccession = null;

      if (oldSampleSraAccessions.size() > 0) {
        oldSampleSraAccession = oldSampleSraAccessions.get(0);
      }

      if (newSampleSraAccession == null) {
        newSampleSraAccession =
            Objects.requireNonNullElseGet(
                oldSampleSraAccession,
                () -> Attribute.build(SRA_ACCESSION, generateOneSRAAccession()));
        newSampleAttributes.add(newSampleSraAccession);
      }

      if (oldSampleSraAccession != null
          && !oldSampleSraAccession.getValue().equals(newSampleSraAccession.getValue())) {
        if (!isWebinSuperUser) {
          throw new GlobalExceptions.ChangedSRAAccessionException();
        }
      }
    } else { // old sample doesn't exist, super user submission with accession but no sample exists,
      // example: new samples from NCBI pipeline
      if (newSampleSraAccession == null) {
        newSampleSraAccession = Attribute.build(SRA_ACCESSION, generateOneSRAAccession());
        newSampleAttributes.add(newSampleSraAccession);
      }
    }
  }

  private Instant defineCreateDate(
      final Sample newSample, final Sample oldSample, final AuthorizationProvider authProvider) {
    final String domain = newSample.getDomain();

    if (authProvider == AuthorizationProvider.WEBIN) {
      return (oldSample.getCreate() != null ? oldSample.getCreate() : newSample.getCreate());
    } else {
      if (isPipelineNcbiDomain(domain)) {
        return newSample.getCreate() != null
            ? newSample.getCreate()
            : (oldSample.getCreate() != null ? oldSample.getCreate() : oldSample.getUpdate());
      } else if (isPipelineEnaDomain(domain)) {
        return oldSample.getCreate() != null ? oldSample.getCreate() : newSample.getCreate();
      } else {
        return oldSample.getCreate() != null ? oldSample.getCreate() : newSample.getCreate();
      }
    }
  }

  private boolean isPipelineEnaDomain(final String domain) {
    if (domain == null) {
      return false;
    }

    return domain.equalsIgnoreCase(ENA_IMPORT_DOMAIN);
  }

  private boolean isPipelineNcbiDomain(final String domain) {
    if (domain == null) {
      return false;
    }

    return domain.equalsIgnoreCase(NCBI_IMPORT_DOMAIN);
  }

  private Instant defineSubmittedDate(
      final Sample newSample,
      final Sample oldSample,
      final boolean isEmptySample,
      final AuthorizationProvider authProvider) {
    if (authProvider == AuthorizationProvider.WEBIN) {
      if (isEmptySample) {
        return newSample.getSubmitted();
      } else {
        return oldSample.getSubmitted() != null
            ? oldSample.getSubmitted()
            : newSample.getSubmitted();
      }
    } else {
      final String domain = newSample.getDomain();

      if (isPipelineNcbiDomain(domain)) {
        return newSample.getSubmitted() != null
            ? newSample.getSubmitted()
            : (oldSample.getSubmitted() != null ? oldSample.getSubmitted() : oldSample.getCreate());
      } else if (isPipelineEnaDomain(domain)) {
        return (oldSample.getSubmitted() != null)
            ? oldSample.getSubmitted()
            : newSample.getSubmitted();
      } else {
        if (isEmptySample) {
          return newSample.getSubmitted();
        } else {
          return oldSample.getSubmitted() != null
              ? oldSample.getSubmitted()
              : (oldSample.getCreate() != null ? oldSample.getCreate() : oldSample.getUpdate());
        }
      }
    }
  }

  public Instant defineCreateDate(final Sample sample, final boolean isWebinSuperUserSubmission) {
    final Instant now = Instant.now();
    final String domain = sample.getDomain();
    final Instant create = sample.getCreate();

    return ((domain != null && isAPipelineAapDomain(domain)) || isWebinSuperUserSubmission)
        ? (create != null ? create : now)
        : now;
  }

  public Instant defineSubmittedDate(
      final Sample sample, final boolean isWebinSuperUserSubmission) {
    final Instant now = Instant.now();
    final String domain = sample.getDomain();
    final Instant submitted = sample.getSubmitted();

    return ((domain != null && isAPipelineAapDomain(domain)) || isWebinSuperUserSubmission)
        ? (submitted != null ? submitted : now)
        : now;
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
      if (sampleRelationships != null && sampleRelationships.size() > 0) {
        throw new GlobalExceptions.SampleWithRelationshipSubmissionExceptionV2();
      }
    }

    if (sample.hasAccession()) {
      if (oldSampleOptional.isPresent()) {
        final Sample oldSample = oldSampleOptional.get();

        if (oldSample.getRelationships() != null && oldSample.getRelationships().size() > 0) {
          return Stream.of(oldSample.getRelationships(), sampleRelationships)
              .filter(Objects::nonNull)
              .flatMap(Set::stream)
              .collect(toSet());
        }
      } else {
        return sampleRelationships;
      }
    } else {
      if (sampleRelationships != null && sampleRelationships.size() > 0) {
        throw new GlobalExceptions.SampleWithRelationshipSubmissionExceptionV2();
      }
    }

    return null;
  }

  public Optional<Sample> validateSampleWithAccessionsAgainstConditionsAndGetOldSample(
      final Sample sample, final boolean anySuperUser) {
    if (!anySuperUser) {
      if (sample.hasAccession()) {
        throw new GlobalExceptions.SampleWithAccessionSubmissionException();
      }

      if (sample.getAttributes() != null
          && sample.getAttributes().stream()
              .anyMatch(attribute -> attribute.getType().equalsIgnoreCase(SRA_ACCESSION))) {
        throw new GlobalExceptions.SampleWithAccessionSubmissionException();
      }
    } else {
      if (sample.hasAccession()) {
        final boolean nonExistingAccession = isNotExistingAccession(sample.getAccession());

        if (!nonExistingAccession) {
          // fetch old sample if sample exists
          return fetch(sample.getAccession(), Optional.empty());
        }
      }
    }

    return Optional.empty();
  }
}
