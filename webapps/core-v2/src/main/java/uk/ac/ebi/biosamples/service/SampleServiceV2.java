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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.ResponseStatus;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.StaticViewWrapper;
import uk.ac.ebi.biosamples.mongo.model.MongoSample;
import uk.ac.ebi.biosamples.mongo.repo.MongoSampleRepository;
import uk.ac.ebi.biosamples.mongo.service.MongoAccessionService;
import uk.ac.ebi.biosamples.mongo.service.MongoSampleToSampleConverter;
import uk.ac.ebi.biosamples.mongo.service.SampleToMongoSampleConverter;

/**
 * Service layer business logic for centralising repository access and conversions between different
 * controller. Use this instead of linking to repositories directly.
 */
@Service
public class SampleServiceV2 {
  private static Logger log = LoggerFactory.getLogger(SampleServiceV2.class);
  private static final String NCBI_IMPORT_DOMAIN = "self.BiosampleImportNCBI";
  private static final String ENA_IMPORT_DOMAIN = "self.BiosampleImportENA";

  @Qualifier("SampleAccessionService")
  @Autowired
  private MongoAccessionService mongoAccessionService;

  @Autowired private MongoSampleRepository mongoSampleRepository;
  @Autowired private MongoSampleToSampleConverter mongoSampleToSampleConverter;
  @Autowired private SampleToMongoSampleConverter sampleToMongoSampleConverter;
  @Autowired private SampleValidator sampleValidator;
  @Autowired private SampleReadService sampleReadService;

  /**
   * Throws an IllegalArgumentException of no sample with that accession exists
   *
   * @param accession the sample accession
   * @return
   * @throws IllegalArgumentException
   */
  public Optional<Sample> fetch(
      final String accession,
      final Optional<List<String>> curationDomains,
      final String curationRepo) {
    StaticViewWrapper.StaticView staticView =
        StaticViewWrapper.getStaticView(curationDomains.orElse(null), curationRepo);
    return sampleReadService.fetch(accession, curationDomains, staticView);
  }

  public boolean beforeStore(final Sample sample) {
    return beforeStoreCheck(sample);
  }

  private boolean beforeStoreCheck(final Sample sample) {
    final boolean firstTimeMetadataAdded = isFirstTimeMetadataAddedForNonImportedSamples(sample);

    if (firstTimeMetadataAdded) {
      log.trace("First time metadata added");
    }

    return firstTimeMetadataAdded;
  }

  private boolean isFirstTimeMetadataAddedForNonImportedSamples(final Sample sample) {
    boolean firstTimeMetadataAdded = true;

    if (sample.hasAccession()) {
      final MongoSample mongoOldSample = mongoSampleRepository.findOne(sample.getAccession());

      if (mongoOldSample != null) {
        firstTimeMetadataAdded = isFirstTimeMetadataAdded(mongoOldSample);
      }
    }

    return firstTimeMetadataAdded;
  }

  public boolean isPipelineEnaOrNcbiDomain(String domain) {
    return isPipelineEnaDomain(domain) || isPipelineNcbiDomain(domain);
  }

  private boolean isPipelineEnaDomain(String domain) {
    if (domain == null) return false;
    return domain.equalsIgnoreCase(ENA_IMPORT_DOMAIN);
  }

  private boolean isPipelineNcbiDomain(String domain) {
    if (domain == null) return false;
    return domain.equalsIgnoreCase(NCBI_IMPORT_DOMAIN);
  }

  private boolean isFirstTimeMetadataAdded(final MongoSample mongoOldSample) {
    boolean firstTimeMetadataAdded = true;
    final Sample oldSample = mongoSampleToSampleConverter.convert(mongoOldSample);

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

    return firstTimeMetadataAdded;
  }

  public boolean isNotExistingAccession(final String accession) {
    return mongoSampleRepository.findOne(accession) == null;
  }

  public Sample store(
      Sample sample, final boolean isFirstTimeMetadataAdded, final String authProvider) {
    final Collection<String> errors = sampleValidator.validate(sample);

    if (!errors.isEmpty()) {
      log.error("Sample validation failed : {}", errors);
      throw new SampleValidationException(String.join("|", errors));
    }

    if (sample.hasAccession()) {
      final MongoSample mongoOldSample = mongoSampleRepository.findOne(sample.getAccession());

      if (mongoOldSample != null) {
        final Sample oldSample = mongoSampleToSampleConverter.convert(mongoOldSample);

        sample =
            compareWithExistingAndUpdateSample(
                sample, oldSample, isFirstTimeMetadataAdded, authProvider);
      } else {
        log.error("Trying to update sample not in database, accession: {}", sample.getAccession());
      }

      MongoSample mongoSample = sampleToMongoSampleConverter.convert(sample);

      mongoSample = mongoSampleRepository.save(mongoSample);
      sample = mongoSampleToSampleConverter.convert(mongoSample);
    } else {
      sample = mongoAccessionService.generateAccession(sample);
    }
    // do a fetch to return it with accession, curation objects, inverse relationships
    return fetch(sample.getAccession(), Optional.empty(), null).get();
  }

  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public static class SampleValidationException extends RuntimeException {
    private static final long serialVersionUID = -7937033504537036300L;

    public SampleValidationException(final String message) {
      super(message);
    }
  }

  private Sample compareWithExistingAndUpdateSample(
      final Sample sampleToUpdate,
      final Sample oldSample,
      final boolean isFirstTimeMetadataAdded,
      final String authProvider) {
    // compare with existing version and check what fields have changed
    if (sampleToUpdate.equals(oldSample)) {
      log.info("New sample is similar to the old sample, accession: {}", oldSample.getAccession());
    }

    return Sample.Builder.fromSample(sampleToUpdate)
        .withCreate(defineCreateDate(sampleToUpdate, oldSample))
        .withSubmitted(
            defineSubmittedDate(sampleToUpdate, oldSample, isFirstTimeMetadataAdded, authProvider))
        .build();
  }

  private Instant defineCreateDate(final Sample sampleToUpdate, final Sample oldSample) {
    return (oldSample.getCreate() != null ? oldSample.getCreate() : sampleToUpdate.getCreate());
  }

  public boolean isWebinAuthentication(final String authProviderIdentifier) {
    return authProviderIdentifier != null && authProviderIdentifier.equalsIgnoreCase("WEBIN");
  }

  private Instant defineSubmittedDate(
      final Sample sampleToUpdate,
      final Sample oldSample,
      final boolean isFirstTimeMetadataAdded,
      final String authProvider) {
    if (isWebinAuthentication(authProvider)) {
      if (isFirstTimeMetadataAdded) {
        return sampleToUpdate.getSubmitted();
      } else {
        return oldSample.getSubmitted() != null
            ? oldSample.getSubmitted()
            : sampleToUpdate.getSubmitted();
      }
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
