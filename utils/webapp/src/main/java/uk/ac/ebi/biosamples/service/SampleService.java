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
import uk.ac.ebi.biosamples.exceptions.GlobalExceptions;
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
import uk.ac.ebi.biosamples.mongo.service.MongoSampleToSampleConverter;
import uk.ac.ebi.biosamples.mongo.service.SampleToMongoSampleConverter;
import uk.ac.ebi.biosamples.utils.mongo.SampleReadService;

/**
 * Service layer business logic for centralising repository access and conversions between different
 * controller. Use this instead of linking to repositories directly.
 *
 * @author faulcon
 */
@Service
public class SampleService {
  private static final String NCBI_IMPORT_DOMAIN = "self.BiosampleImportNCBI";
  private static final String ENA_IMPORT_DOMAIN = "self.BiosampleImportENA";
  private static final String ENA_CHECKLIST = "ENA-CHECKLIST";
  private static final Logger log = LoggerFactory.getLogger(SampleService.class);

  @Qualifier("SampleAccessionService")
  @Autowired
  private MongoAccessionService mongoAccessionService;

  @Autowired private MongoSampleRepository mongoSampleRepository;
  @Autowired private MongoSampleMessageRepository mongoSampleMessageRepository;
  @Autowired private MongoSampleToSampleConverter mongoSampleToSampleConverter;
  @Autowired private SampleToMongoSampleConverter sampleToMongoSampleConverter;
  @Autowired private SampleValidator sampleValidator;
  @Autowired private SampleReadService sampleReadService;
  @Autowired private MessagingService messagingService;

  /** Throws an IllegalArgumentException of no sample with that accession exists */
  public Optional<Sample> fetch(
      final String accession, final Optional<List<String>> curationDomains) {
    return sampleReadService.fetch(accession, curationDomains);
  }

  /*
  Checks if the current sample that exists has no metadata, returns true if empty
   */
  private boolean isExistingSampleEmpty(
      final Sample sample, final boolean isWebinSuperUser, final Sample oldSample) {
    final String domain = sample.getDomain();

    if (isWebinSuperUser) {
      if (sample.getSubmittedVia() == SubmittedViaType.FILE_UPLOADER) {
        // file uploader submissions are done via super user but they are non imported samples,
        // needs to be handled safely
        if (sample.hasAccession()) {
          return isExistingSampleEmpty(oldSample);
        }

        return true;
      } else {
        // otherwise it is a ENA pipeline import, cannot be empty
        return false;
      }
    }

    if (isAPipelineAapDomain(domain)) {
      return false; // imported sample - never submitted first time to BSD, always has metadata
    } else {
      if (sample.hasAccession()) {
        return isExistingSampleEmpty(oldSample);
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
  private boolean isExistingSampleEmpty(final Sample oldSample) {
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
      List<String> existingRelationshipTargets = new ArrayList<>();

      if (oldSample != null) {
        final boolean isExistingSampleEmpty =
            isExistingSampleEmpty(sample, isWebinSuperUser, oldSample);

        if (isExistingSampleEmpty) {
          sample = Sample.Builder.fromSample(sample).withSubmitted(Instant.now()).build();
        }

        existingRelationshipTargets =
            getExistingRelationshipTargets(
                sample.getAccession(),
                Objects.requireNonNull(sampleToMongoSampleConverter.convert(oldSample)));

        sample =
            compareWithExistingAndUpdateSample(
                sample, oldSample, isExistingSampleEmpty, authProvider);

        final Long oldSampleTaxId = oldSample.getTaxId();

        if (oldSampleTaxId != null && !oldSampleTaxId.equals(taxId)) {
          isSampleTaxIdUpdated = true;
        }
      } else {
        log.error("Trying to update sample not in database, accession: {}", sample.getAccession());
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
      messagingService.fetchThenSendMessage(sample.getAccession(), existingRelationshipTargets);
    } else {
      sample = mongoAccessionService.generateAccession(sample);
      messagingService.fetchThenSendMessage(sample.getAccession());
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

        final boolean isExistingSampleEmpty =
            isExistingSampleEmpty(sample, isWebinSuperUser, oldSample);

        if (isExistingSampleEmpty) {
          sample = Sample.Builder.fromSample(sample).withSubmitted(Instant.now()).build();
        }

        sample =
            compareWithExistingAndUpdateSample(
                sample, oldSample, isExistingSampleEmpty, authProvider);
      } else {
        log.error("Trying to update sample not in database, accession: {}", sample.getAccession());
      }

      MongoSample mongoSample = sampleToMongoSampleConverter.convert(sample);

      assert mongoSample != null;

      mongoSample = mongoSampleRepository.save(mongoSample);
      sample = mongoSampleToSampleConverter.apply(mongoSample);
    } else {
      sample = mongoAccessionService.generateAccession(sample);
    }

    return sample;
  }

  /*
  Called by V2 endpoints to build a sample with a newly generated sample accession
   */
  public Sample accessionSample(final Sample sample) {
    final Collection<String> errors = sampleValidator.validate(sample);

    if (!errors.isEmpty()) {
      log.error("Sample validation failed : {}", errors);
      throw new GlobalExceptions.SampleValidationControllerException(String.join("|", errors));
    }

    return mongoAccessionService.generateAccession(sample);
  }

  /*
  Returns true if a sample does not exist in BioSamples
   */
  public boolean isNotExistingAccession(final String accession) {
    if (accession != null) {
      return !mongoSampleRepository.findById(accession).isPresent();
    } else {
      return true;
    }
  }

  private List<String> getExistingRelationshipTargets(
      final String accession, final MongoSample mongoOldSample) {
    final List<String> oldRelationshipTargets = new ArrayList<>();
    for (final MongoRelationship relationship : mongoOldSample.getRelationships()) {
      if (relationship.getSource().equals(accession)) {
        oldRelationshipTargets.add(relationship.getTarget());
      }
    }

    return oldRelationshipTargets;
  }

  private Sample compareWithExistingAndUpdateSample(
      final Sample newSample,
      final Sample oldSample,
      final boolean isEmptySample,
      final AuthorizationProvider authProvider) {
    Set<AbstractData> structuredData = new HashSet<>();
    boolean applyOldSampleStructuredData = false;

    isImportedSampleUpdatedByNonPipelineSource(newSample, oldSample);

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

  private void isImportedSampleUpdatedByNonPipelineSource(
      final Sample newSample, final Sample oldSample) {
    /*
    Old sample has ENA-CHECKLIST attribute, hence it can be concluded that it is imported from ENA
    New sample has ENA-CHECKLIST attribute, means its updated by ENA pipeline, allow further computation
    New sample doesn't have ENA-CHECKLIST attribute, means it's not updated by ENA pipeline, don't allow further computation and throw exception
     */
    if (oldSample.getAttributes().stream()
        .anyMatch(attribute -> attribute.getType().equalsIgnoreCase(ENA_CHECKLIST))) {
      if (newSample.getAttributes().stream()
          .noneMatch(attribute -> attribute.getType().equalsIgnoreCase(ENA_CHECKLIST))) {
        throw new GlobalExceptions.InvalidSubmissionSourceException();
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

  public void validateSampleHasNoRelationshipsV2(final Sample sample) {
    if (sample.getRelationships().size() > 0) {
      throw new GlobalExceptions.SampleWithRelationshipSubmissionExceptionV2();
    }
  }
}
