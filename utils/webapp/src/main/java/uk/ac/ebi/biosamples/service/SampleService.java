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

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
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
import uk.ac.ebi.biosamples.mongo.model.MongoRelationship;
import uk.ac.ebi.biosamples.mongo.model.MongoSample;
import uk.ac.ebi.biosamples.mongo.model.MongoSampleMessage;
import uk.ac.ebi.biosamples.mongo.repo.MongoSampleMessageRepository;
import uk.ac.ebi.biosamples.mongo.repo.MongoSampleRepository;
import uk.ac.ebi.biosamples.mongo.service.MongoAccessionService;
import uk.ac.ebi.biosamples.mongo.service.MongoRelationshipToRelationshipConverter;
import uk.ac.ebi.biosamples.mongo.service.MongoSampleToSampleConverter;
import uk.ac.ebi.biosamples.mongo.service.SampleToMongoSampleConverter;
import uk.ac.ebi.biosamples.service.security.BioSamplesCrossSourceIngestAccessControlService;
import uk.ac.ebi.biosamples.utils.mongo.SampleReadService;

/**
 * Service layer business logic for centralising repository access and conversions between different
 * controller. Use this instead of linking to repositories directly.
 *
 * @author faulcon
 */
@Service
public class SampleService {
  private static final Logger log = LoggerFactory.getLogger(SampleService.class);
  private static final String NCBI_IMPORT_DOMAIN = "self.BiosampleImportNCBI";
  private static final String ENA_IMPORT_DOMAIN = "self.BiosampleImportENA";
  private static final String SRA_ACCESSION = "SRA accession";

  @Qualifier("SampleAccessionService")
  @Autowired
  private MongoAccessionService mongoAccessionService;

  @Autowired private MongoSampleRepository mongoSampleRepository;
  @Autowired private MongoSampleMessageRepository mongoSampleMessageRepository;
  @Autowired private MongoSampleToSampleConverter mongoSampleToSampleConverter;
  @Autowired private SampleToMongoSampleConverter sampleToMongoSampleConverter;

  @Autowired
  private MongoRelationshipToRelationshipConverter mongoRelationshipToRelationshipConverter;

  @Autowired private SampleValidator sampleValidator;
  @Autowired private SampleReadService sampleReadService;
  @Autowired private MessagingService messagingService;
  @Autowired private BioSamplesProperties bioSamplesProperties;

  @Autowired
  private BioSamplesCrossSourceIngestAccessControlService
      bioSamplesCrossSourceIngestAccessControlService;

  /** Throws an IllegalArgumentException of no sample with that accession exists */
  public Optional<Sample> fetch(
      final String accession, final Optional<List<String>> curationDomains) {
    return sampleReadService.fetch(accession, curationDomains);
  }

  /*
  Checks if the current sample that exists has no metadata, returns true if empty
   */
  private boolean isSavedSampleEmpty(
      final Sample sample, final boolean isWebinSuperUser, final Sample oldSample) {
    final String domain = sample.getDomain();

    if (isWebinSuperUser) {
      if (sample.getSubmittedVia() == SubmittedViaType.FILE_UPLOADER) {
        // file uploader submissions are done via super-user, but they are non imported samples,
        // needs to be handled safely
        if (sample.hasAccession()) {
          return isSavedSampleEmpty(oldSample);
        }

        return true;
      } else {
        // otherwise it is a ENA pipeline import, cannot be empty
        return false;
      }
    } else {
      if (sample.hasAccession()) {
        return isSavedSampleEmpty(oldSample);
      }
    }

    if (isAPipelineAapDomain(domain)) {
      return false; // imported sample - never submitted first time to BSD, always has metadata
    } else {
      if (sample.hasAccession()) {
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

  // because the fetchUsing caches the sample, if an updated version is stored, we need to make
  // sure
  // that any cached version
  // is removed.
  // Note, pages of samples will not be cache busted, only single-accession sample retrieval
  // @CacheEvict(cacheNames=WebappProperties.fetchUsing, key="#result.accession")
  /*
  Called by V1 endpoints to persist samples
   */
  public Sample persistSample(
      Sample sample,
      final Sample oldSample,
      final AuthorizationProvider authProvider,
      final boolean isWebinSuperUser) {
    boolean isSampleTaxIdUpdated = false;
    final Collection<String> errors = sampleValidator.validate(sample);

    if (!errors.isEmpty()) {
      log.error("Sample validation failed : {}", errors);
      throw new GlobalExceptions.SampleValidationControllerException(String.join("|", errors));
    }

    if (sample.hasAccession()) {
      final Long taxId = sample.getTaxId();
      final List<String> existingRelationshipTargets = new ArrayList<>();

      if (oldSample != null) {
        final boolean savedSampleEmpty = isSavedSampleEmpty(sample, isWebinSuperUser, oldSample);

        if (savedSampleEmpty) {
          sample = Sample.Builder.fromSample(sample).withSubmitted(Instant.now()).build();
        }

        final Sample finalSample = sample;

        final List<Relationship> existingRelationships =
            getExistingRelationshipTargets(
                sample.getAccession(),
                Objects.requireNonNull(sampleToMongoSampleConverter.convert(oldSample)));

        existingRelationshipTargets.addAll(
            existingRelationships.stream()
                .map(
                    relationship -> {
                      if (relationship.getSource().equals(finalSample.getAccession())) {
                        return relationship.getTarget();
                      }

                      return null;
                    })
                .toList());

        sample =
            compareWithExistingAndUpdateSample(
                sample, oldSample, existingRelationships, savedSampleEmpty, authProvider);

        final Long oldSampleTaxId = oldSample.getTaxId();

        if (oldSampleTaxId != null && !oldSampleTaxId.equals(taxId)) {
          isSampleTaxIdUpdated = true;
        }
      } else {
        log.error("Trying to update sample not in database, accession: {}", sample.getAccession());

        bioSamplesCrossSourceIngestAccessControlService
            .validateFileUploaderSampleAccessionWhileSampleUpdate(sample);
      }

      MongoSample mongoSample = sampleToMongoSampleConverter.convert(sample);

      assert mongoSample != null;
      mongoSample = mongoSampleRepository.save(mongoSample);

      if (isSampleTaxIdUpdated) {
        mongoSampleMessageRepository.save(
            new MongoSampleMessage(sample.getAccession(), Instant.now(), taxId));
      }

      sample = mongoSampleToSampleConverter.apply(mongoSample);

      // send a message for storage and further processing, send relationship targets to
      // identify
      // deleted relationships
      sendMessageToRabbitForIndexingToSolr(sample.getAccession(), existingRelationshipTargets);
    } else {
      final boolean sampleNotSraAccessioned =
          sample.getAttributes().stream()
              .noneMatch(attribute -> attribute.getType().equals(SRA_ACCESSION));

      sample = mongoAccessionService.generateAccession(sample, sampleNotSraAccessioned);
      sendMessageToRabbitForIndexingToSolr(sample.getAccession(), Collections.emptyList());
    }

    // do a fetch to return it with accession, curation objects, inverse relationships
    final Optional<Sample> sampleOptional = fetch(sample.getAccession(), Optional.empty());

    if (sampleOptional.isPresent()) {
      final Sample fetchedSample = sampleOptional.get();

      if (fetchedSample.getAccession() != null) {
        return fetchedSample;
      } else {
        throw new RuntimeException(
            "Failed to create sample. Please contact the BioSamples Helpdesk at biosamples@ebi.ac.uk");
      }
    }

    return null;
  }

  /*
  Called by V2 endpoints to persist samples
   */
  public Sample persistSampleV2(
      Sample sample,
      final Sample oldSample,
      final AuthorizationProvider authProvider,
      final boolean isWebinSuperUser) {
    final Collection<String> errors = sampleValidator.validate(sample);

    if (!errors.isEmpty()) {
      log.error("Sample validation failed : {}", errors);
      throw new GlobalExceptions.SampleValidationControllerException(String.join("|", errors));
    }

    if (sample.hasAccession()) {
      if (oldSample != null) {
        log.info(
            "Trying to update sample that exists in database, accession: {}",
            sample.getAccession());

        final boolean savedSampleEmpty = isSavedSampleEmpty(sample, isWebinSuperUser, oldSample);

        if (savedSampleEmpty) {
          sample = Sample.Builder.fromSample(sample).withSubmitted(Instant.now()).build();
        }

        sample =
            compareWithExistingAndUpdateSample(
                sample, oldSample, null, savedSampleEmpty, authProvider);
      } else {
        log.error("Trying to update sample not in database, accession: {}", sample.getAccession());
      }

      MongoSample mongoSample = sampleToMongoSampleConverter.convert(sample);

      assert mongoSample != null;

      mongoSample = mongoSampleRepository.save(mongoSample);
      sample = mongoSampleToSampleConverter.apply(mongoSample);

      sendMessageToRabbitForIndexingToSolr(sample.getAccession(), Collections.emptyList());
    } else {
      final boolean sampleNotSraAccessioned =
          sample.getAttributes().stream()
              .noneMatch(attribute -> attribute.getType().equals(SRA_ACCESSION));

      sample = mongoAccessionService.generateAccession(sample, sampleNotSraAccessioned);

      sendMessageToRabbitForIndexingToSolr(sample.getAccession(), Collections.emptyList());
    }

    return sample;
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
  public Sample accessionSample(Sample sample) {
    final Collection<String> errors = sampleValidator.validate(sample);

    if (!errors.isEmpty()) {
      log.error("Sample validation failed : {}", errors);
      throw new GlobalExceptions.SampleValidationControllerException(String.join("|", errors));
    }

    if (sample
        .getWebinSubmissionAccountId()
        .equalsIgnoreCase(bioSamplesProperties.getBiosamplesClientWebinUsername())) {
      // accessioning from ENA, sample name is the SRA accession here
      final Attribute sraAccessionAttribute = Attribute.build(SRA_ACCESSION, sample.getName());

      sample.getAttributes().add(sraAccessionAttribute);
      sample = Sample.Builder.fromSample(sample).build();

      return mongoAccessionService.generateAccession(sample, false);
    } else {
      return mongoAccessionService.generateAccession(sample, true);
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

  private List<Relationship> getExistingRelationshipTargets(
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
      final AuthorizationProvider authProvider) {
    Set<AbstractData> structuredData = new HashSet<>();
    boolean applyOldSampleStructuredData = false;

    // retain existing relationships for supre user submissions, pipelines, ENA POSTED, not for file
    // uploads though
    handleRelationships(newSample, existingRelationships, authProvider);
    handleSRAAccession(newSample, oldSample);

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
      final Sample newSample,
      final List<Relationship> existingRelationships,
      final AuthorizationProvider authProvider) {
    if (authProvider == AuthorizationProvider.WEBIN
        && newSample
            .getWebinSubmissionAccountId()
            .equals(bioSamplesProperties.getBiosamplesClientWebinUsername())) {
      if (newSample.getSubmittedVia() != SubmittedViaType.FILE_UPLOADER) {
        newSample.getRelationships().addAll(existingRelationships);
      }
    }
  }

  private void handleSRAAccession(final Sample newSample, final Sample oldSample) {
    final List<Attribute> oldSampleSraAccessions =
        oldSample.getAttributes().stream()
            .filter(attribute -> attribute.getType().equalsIgnoreCase(SRA_ACCESSION))
            .toList();
    final SortedSet<Attribute> newSampleAttributes = newSample.getAttributes();
    final List<Attribute> newSampleSraAccessions =
        newSampleAttributes.stream()
            .filter(attribute -> attribute.getType().equalsIgnoreCase(SRA_ACCESSION))
            .toList();

    if (oldSampleSraAccessions.size() > 1) {
      throw new GlobalExceptions.InvalidSampleException();
    }

    if (newSampleSraAccessions.size() > 1) {
      throw new GlobalExceptions.InvalidSampleException();
    }

    Attribute oldSampleSraAccession = null;
    Attribute newSampleSraAccession = null;

    if (oldSampleSraAccessions.size() > 0) {
      oldSampleSraAccession = oldSampleSraAccessions.get(0);
    }

    if (newSampleSraAccessions.size() > 0) {
      newSampleSraAccession = newSampleSraAccessions.get(0);
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
      throw new GlobalExceptions.ChangedSRAAccessionException();
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

  public void validateSampleHasNoRelationshipsV2(final Sample sample) {
    if (sample.getRelationships().size() > 0) {
      throw new GlobalExceptions.SampleWithRelationshipSubmissionExceptionV2();
    }
  }
}
