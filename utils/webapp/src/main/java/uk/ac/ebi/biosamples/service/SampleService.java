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
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.exceptions.GlobalExceptions;
import uk.ac.ebi.biosamples.model.*;
import uk.ac.ebi.biosamples.model.auth.AuthorizationProvider;
import uk.ac.ebi.biosamples.model.filter.Filter;
import uk.ac.ebi.biosamples.model.structured.AbstractData;
import uk.ac.ebi.biosamples.mongo.model.MongoRelationship;
import uk.ac.ebi.biosamples.mongo.model.MongoSample;
import uk.ac.ebi.biosamples.mongo.model.MongoSampleMessage;
import uk.ac.ebi.biosamples.mongo.repo.MongoSampleMessageRepository;
import uk.ac.ebi.biosamples.mongo.repo.MongoSampleRepository;
import uk.ac.ebi.biosamples.mongo.service.MongoAccessionService;
import uk.ac.ebi.biosamples.mongo.service.MongoSampleToSampleConverter;
import uk.ac.ebi.biosamples.mongo.service.SampleToMongoSampleConverter;
import uk.ac.ebi.biosamples.solr.service.SolrSampleService;
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
  private static Logger log = LoggerFactory.getLogger(SampleService.class);

  @Qualifier("SampleAccessionService")
  @Autowired
  private MongoAccessionService mongoAccessionService;

  @Autowired private MongoSampleRepository mongoSampleRepository;
  @Autowired private MongoSampleMessageRepository mongoSampleMessageRepository;
  @Autowired private MongoSampleToSampleConverter mongoSampleToSampleConverter;
  @Autowired private SampleToMongoSampleConverter sampleToMongoSampleConverter;
  @Autowired private SampleValidator sampleValidator;
  @Autowired private SolrSampleService solrSampleService;
  @Autowired private SampleReadService sampleReadService;
  @Autowired private MessagingService messagingSerivce;

  /**
   * Throws an IllegalArgumentException of no sample with that accession exists
   *
   * @param accession the sample accession
   * @return
   * @throws IllegalArgumentException
   */
  public Optional<Sample> fetch(
      String accession, Optional<List<String>> curationDomains, String curationRepo) {
    StaticViewWrapper.StaticView staticView =
        StaticViewWrapper.getStaticView(curationDomains.orElse(null), curationRepo);
    return sampleReadService.fetch(accession, curationDomains, staticView);
  }

  public Autocomplete getAutocomplete(
      String autocompletePrefix, Collection<Filter> filters, int noSuggestions) {
    return solrSampleService.getAutocomplete(autocompletePrefix, filters, noSuggestions);
  }

  public boolean checkIfSampleHasMetadata(Sample sample, boolean isWebinSuperUser) {
    boolean isMetadataSubmitted;
    final String domain = sample.getDomain();
    boolean isMetadataSubmittedIfSubmitterIsWebinSuperUser = true;

    if (isWebinSuperUser) {
      if (sample.getSubmittedVia() == SubmittedViaType.FILE_UPLOADER) {
        // file uploader submissions are done via super user but they are non imported samples,
        // needs to be handled safely
        isMetadataSubmittedIfSubmitterIsWebinSuperUser =
            checkIfNonImportedSampleHasMetadata(sample);
      }
    }

    if (isAnImportAapDomain(domain) || isMetadataSubmittedIfSubmitterIsWebinSuperUser)
      isMetadataSubmitted = false; // imported sample - never submitted first time to BSD
    else {
      isMetadataSubmitted = checkIfNonImportedSampleHasMetadata(sample);
    }

    return isMetadataSubmitted;
  }

  private boolean checkIfNonImportedSampleHasMetadata(Sample sample) {
    boolean firstTimeMetadataAdded = true;

    if (sample.hasAccession()) {
      final Optional<MongoSample> byId = mongoSampleRepository.findById(sample.getAccession());
      final MongoSample mongoOldSample = byId.orElse(null);

      if (mongoOldSample != null) {
        firstTimeMetadataAdded = isEmptySample(mongoOldSample);
      }
    }

    return firstTimeMetadataAdded;
  }

  public boolean isAnImportAapDomain(String domain) {
    return isPipelineEnaDomain(domain) || isPipelineNcbiDomain(domain);
  }

  private boolean isEmptySample(MongoSample mongoOldSample) {
    boolean firstTimeMetadataAdded = true;
    Sample oldSample = mongoSampleToSampleConverter.apply(mongoOldSample);

    if (oldSample.getTaxId() != null && oldSample.getTaxId() > 0) {
      firstTimeMetadataAdded = false;
    }

    if (oldSample.getAttributes().size() > 0) {
      firstTimeMetadataAdded = false;
    }

    if (oldSample.getRelationships().size() > 0) {
      firstTimeMetadataAdded = false;
    }

    if (oldSample.getPublications().size() > 0) {
      firstTimeMetadataAdded = false;
    }

    if (oldSample.getContacts().size() > 0) {
      firstTimeMetadataAdded = false;
    }

    if (oldSample.getOrganizations().size() > 0) {
      firstTimeMetadataAdded = false;
    }

    if (oldSample.getData().size() > 0) {
      firstTimeMetadataAdded = false;
    }

    if (oldSample.getExternalReferences().size() > 0) {
      firstTimeMetadataAdded = false;
    }

    if (oldSample.getStructuredData().size() > 0) {
      firstTimeMetadataAdded = false;
    }

    return firstTimeMetadataAdded;
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
      Sample sample, boolean isNewOrPreRegisteredSample, AuthorizationProvider authProvider) {
    boolean isSampleTaxIdUpdated = false;
    Collection<String> errors = sampleValidator.validate(sample);

    if (!errors.isEmpty()) {
      log.error("Sample validation failed : {}", errors);
      throw new GlobalExceptions.SampleValidationControllerException(String.join("|", errors));
    }

    if (sample.hasAccession()) {
      final Optional<MongoSample> byId = mongoSampleRepository.findById(sample.getAccession());
      final MongoSample mongoOldSample = byId.orElse(null);

      List<String> existingRelationshipTargets = new ArrayList<>();

      final Long taxId = sample.getTaxId();

      if (mongoOldSample != null) {
        final Sample oldSample = mongoSampleToSampleConverter.apply(mongoOldSample);

        existingRelationshipTargets =
            getExistingRelationshipTargets(sample.getAccession(), mongoOldSample);

        sample =
            compareWithExistingAndUpdateSample(
                sample, oldSample, isNewOrPreRegisteredSample, authProvider);

        if (oldSample.getTaxId() != null && !oldSample.getTaxId().equals(taxId)) {
          isSampleTaxIdUpdated = true;
        }
      } else {
        log.error("Trying to update sample not in database, accession: {}", sample.getAccession());
      }

      MongoSample mongoSample = sampleToMongoSampleConverter.convert(sample);

      mongoSample = mongoSampleRepository.save(mongoSample);

      if (isSampleTaxIdUpdated) {
        mongoSampleMessageRepository.save(
            new MongoSampleMessage(sample.getAccession(), Instant.now(), taxId));
      }

      sample = mongoSampleToSampleConverter.apply(mongoSample);

      // send a message for storage and further processing, send relationship targets to
      // identify
      // deleted relationships
      messagingSerivce.fetchThenSendMessage(sample.getAccession(), existingRelationshipTargets);
    } else {
      sample = mongoAccessionService.generateAccession(sample);
      messagingSerivce.fetchThenSendMessage(sample.getAccession());
    }

    // do a fetch to return it with accession, curation objects, inverse relationships
    return fetch(sample.getAccession(), Optional.empty(), null).get();
  }

  /*
  Called by V2 endpoints to persist samples
   */
  public Sample persistSampleV2(
      Sample sample, boolean isFirstTimeMetadataAdded, AuthorizationProvider authProvider) {
    Collection<String> errors = sampleValidator.validate(sample);

    if (!errors.isEmpty()) {
      log.error("Sample validation failed : {}", errors);
      throw new GlobalExceptions.SampleValidationControllerException(String.join("|", errors));
    }

    if (sample.hasAccession()) {
      final Optional<MongoSample> byId = mongoSampleRepository.findById(sample.getAccession());
      final MongoSample mongoOldSample = byId.orElse(null);

      if (mongoOldSample != null) {
        final Sample oldSample = mongoSampleToSampleConverter.apply(mongoOldSample);

        sample =
            compareWithExistingAndUpdateSample(
                sample, oldSample, isFirstTimeMetadataAdded, authProvider);
      } else {
        log.error("Trying to update sample not in database, accession: {}", sample.getAccession());
      }

      MongoSample mongoSample = sampleToMongoSampleConverter.convert(sample);

      mongoSample = mongoSampleRepository.save(mongoSample);
      sample = mongoSampleToSampleConverter.apply(mongoSample);
    } else {
      sample = mongoAccessionService.generateAccession(sample);
    }

    return sample;
  }

  /*
  Called by both V1 and V2 endpoints to build a sample with a newly generated sample accession
   */
  public Sample accessionSample(Sample sample) {
    Collection<String> errors = sampleValidator.validate(sample);

    if (!errors.isEmpty()) {
      log.error("Sample validation failed : {}", errors);
      throw new GlobalExceptions.SampleValidationControllerException(String.join("|", errors));
    }

    return mongoAccessionService.generateAccession(sample);
  }

  public boolean isNotExistingAccession(String accession) {
    return !mongoSampleRepository.findById(accession).isPresent();
  }

  private List<String> getExistingRelationshipTargets(
      String accession, MongoSample mongoOldSample) {
    List<String> oldRelationshipTargets = new ArrayList<>();
    for (MongoRelationship relationship : mongoOldSample.getRelationships()) {
      if (relationship.getSource().equals(accession)) {
        oldRelationshipTargets.add(relationship.getTarget());
      }
    }

    return oldRelationshipTargets;
  }

  private Sample compareWithExistingAndUpdateSample(
      Sample sampleToUpdate,
      Sample oldSample,
      boolean isFirstTimeMetadataAdded,
      AuthorizationProvider authProvider) {
    Set<AbstractData> structuredData = new HashSet<>();
    boolean applyOldSampleStructuredData = false;

    if (sampleToUpdate.getData().size() < 1) {
      log.info("No structured data in new sample");

      if (oldSample.getData() != null && oldSample.getData().size() > 0) {
        structuredData = oldSample.getData();
        // Check if old sample has structured data, if yes, retain
        applyOldSampleStructuredData = true;

        log.info("Old sample has structured data");
      }
    } else {
      log.info("New sample has structured data");
    }

    if (applyOldSampleStructuredData) {
      log.info("Build sample and applying old sample structured data");
      log.trace("Old sample structured data size is " + structuredData.size());

      return Sample.Builder.fromSample(sampleToUpdate)
          .withCreate(defineCreateDate(sampleToUpdate, oldSample, authProvider))
          .withSubmitted(
              defineSubmittedDate(
                  sampleToUpdate, oldSample, isFirstTimeMetadataAdded, authProvider))
          .withData(structuredData)
          .build();
    } else {
      log.info("Building sample");

      return Sample.Builder.fromSample(sampleToUpdate)
          .withCreate(defineCreateDate(sampleToUpdate, oldSample, authProvider))
          .withSubmitted(
              defineSubmittedDate(
                  sampleToUpdate, oldSample, isFirstTimeMetadataAdded, authProvider))
          .build();
    }
  }

  private Instant defineCreateDate(
      final Sample sampleToUpdate,
      final Sample oldSample,
      final AuthorizationProvider authProvider) {
    final String domain = sampleToUpdate.getDomain();

    if (isWebinAuthentication(authProvider)) {
      return (oldSample.getCreate() != null ? oldSample.getCreate() : sampleToUpdate.getCreate());
    } else {
      if (isPipelineNcbiDomain(domain)) {
        return sampleToUpdate.getCreate() != null
            ? sampleToUpdate.getCreate()
            : (oldSample.getCreate() != null ? oldSample.getCreate() : oldSample.getUpdate());
      } else if (isPipelineEnaDomain(domain)) {
        return oldSample.getCreate() != null ? oldSample.getCreate() : sampleToUpdate.getCreate();
      } else {
        return oldSample.getCreate() != null ? oldSample.getCreate() : sampleToUpdate.getCreate();
      }
    }
  }

  public boolean isWebinAuthentication(AuthorizationProvider authProvider) {
    final String authProviderIdentifier = authProvider.name();

    return authProviderIdentifier.equalsIgnoreCase("WEBIN");
  }

  private boolean isPipelineEnaDomain(String domain) {
    if (domain == null) return false;
    return domain.equalsIgnoreCase(ENA_IMPORT_DOMAIN);
  }

  private boolean isPipelineNcbiDomain(String domain) {
    if (domain == null) return false;
    return domain.equalsIgnoreCase(NCBI_IMPORT_DOMAIN);
  }

  private Instant defineSubmittedDate(
      final Sample sampleToUpdate,
      final Sample oldSample,
      boolean isFirstTimeMetadataAdded,
      AuthorizationProvider authProvider) {
    if (isWebinAuthentication(authProvider)) {
      if (isFirstTimeMetadataAdded) {
        return sampleToUpdate.getSubmitted();
      } else {
        return oldSample.getSubmitted() != null
            ? oldSample.getSubmitted()
            : sampleToUpdate.getSubmitted();
      }
    } else {
      final String domain = sampleToUpdate.getDomain();

      if (isPipelineNcbiDomain(domain)) {
        return sampleToUpdate.getSubmitted() != null
            ? sampleToUpdate.getSubmitted()
            : (oldSample.getSubmitted() != null ? oldSample.getSubmitted() : oldSample.getCreate());
      } else if (isPipelineEnaDomain(domain)) {
        return (oldSample.getSubmitted() != null)
            ? oldSample.getSubmitted()
            : sampleToUpdate.getSubmitted();
      } else {
        if (isFirstTimeMetadataAdded) {
          return sampleToUpdate.getSubmitted();
        } else {
          return oldSample.getSubmitted() != null
              ? oldSample.getSubmitted()
              : (oldSample.getCreate() != null ? oldSample.getCreate() : oldSample.getUpdate());
        }
      }
    }
  }
}
