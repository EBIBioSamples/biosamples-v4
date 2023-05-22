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
package uk.ac.ebi.biosamples.utils.mongo;

import java.time.Instant;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.model.*;
import uk.ac.ebi.biosamples.mongo.model.MongoCuration;
import uk.ac.ebi.biosamples.mongo.model.MongoCurationLink;
import uk.ac.ebi.biosamples.mongo.repo.MongoCurationLinkRepository;
import uk.ac.ebi.biosamples.mongo.repo.MongoCurationRepository;
import uk.ac.ebi.biosamples.mongo.service.MongoCurationLinkToCurationLinkConverter;
import uk.ac.ebi.biosamples.mongo.service.MongoCurationToCurationConverter;

@Service
public class CurationReadService {
  private final Logger log = LoggerFactory.getLogger(getClass());
  @Autowired private MongoCurationRepository mongoCurationRepository;
  @Autowired private MongoCurationLinkRepository mongoCurationLinkRepository;

  // TODO use a ConversionService to manage all these
  @Autowired
  private MongoCurationLinkToCurationLinkConverter mongoCurationLinkToCurationLinkConverter;

  @Autowired private MongoCurationToCurationConverter mongoCurationToCurationConverter;

  public Page<Curation> getPage(final Pageable pageable) {
    final Page<MongoCuration> pageNeoCuration = mongoCurationRepository.findAll(pageable);
    return pageNeoCuration.map(mongoCurationToCurationConverter);
  }

  public Curation getCuration(final String hash) {
    final Optional<MongoCuration> byId = mongoCurationRepository.findById(hash);
    final MongoCuration neoCuration = byId.orElse(null);

    if (neoCuration == null) {
      return null;
    } else {
      return mongoCurationToCurationConverter.apply(neoCuration);
    }
  }

  public Page<CurationLink> getCurationLinksForSample(
      final String accession, final Pageable pageable) {
    final Page<MongoCurationLink> pageNeoCurationLink =
        mongoCurationLinkRepository.findBySample(accession, pageable);
    // convert them into a state to return
    return pageNeoCurationLink.map(mongoCurationLinkToCurationLinkConverter);
  }

  public CurationLink getCurationLink(final String hash) {
    final Optional<MongoCurationLink> byId = mongoCurationLinkRepository.findById(hash);
    final MongoCurationLink mongoCurationLink = byId.orElse(null);

    assert mongoCurationLink != null;
    return mongoCurationLinkToCurationLinkConverter.apply(mongoCurationLink);
  }

  /**
   * This applies a given curation link to a sample and returns a new sample.
   *
   * <p>This needs a curation link rather than a curation object because the samples update date may
   * be modified if the curation link is newer.
   *
   * @param sample
   * @param curationLink
   * @return
   */
  Sample applyCurationLinkToSample(final Sample sample, final CurationLink curationLink) {
    log.trace("Applying curation " + curationLink + " to sample " + sample.getAccession());
    final Curation curation = curationLink.getCuration();

    final SortedSet<Attribute> attributes = new TreeSet<>(sample.getAttributes());
    final SortedSet<ExternalReference> externalReferences =
        new TreeSet<>(sample.getExternalReferences());
    final SortedSet<Relationship> relationships = new TreeSet<>(sample.getRelationships());
    // remove pre-curation things
    for (final Attribute attribute : curation.getAttributesPre()) {
      if (!attributes.contains(attribute)) {
        throw new IllegalArgumentException(
            "Failed to apply curation " + curation + " to sample " + sample);
      }
      attributes.remove(attribute);
    }
    for (final ExternalReference externalReference : curation.getExternalReferencesPre()) {
      if (!externalReferences.contains(externalReference)) {
        throw new IllegalArgumentException(
            "Failed to apply curation " + curation + " to sample " + sample);
      }
      externalReferences.remove(externalReference);
    }
    for (final Relationship relationship : curation.getRelationshipsPre()) {
      if (!relationships.contains(relationship)) {
        throw new IllegalArgumentException(
            "Failed to apply curation " + curation + " to sample " + sample);
      }
      relationships.remove(relationship);
    }
    // add post-curation things
    for (final Attribute attribute : curation.getAttributesPost()) {
      if (attributes.contains(attribute)) {
        throw new IllegalArgumentException(
            "Failed to apply curation " + curation + " to sample " + sample);
      }
      attributes.add(attribute);
    }
    for (final ExternalReference externalReference : curation.getExternalReferencesPost()) {
      if (externalReferences.contains(externalReference)) {
        throw new IllegalArgumentException(
            "Failed to apply curation " + curation + " to sample " + sample);
      }
      externalReferences.add(externalReference);
    }
    for (final Relationship relationship : curation.getRelationshipsPost()) {
      if (relationships.contains(relationship)) {
        throw new IllegalArgumentException(
            "Failed to apply curation " + curation + " to sample " + sample);
      }
      relationships.add(relationship);
    }

    // update the sample's reviewed date
    Instant reviewed = curationLink.getCreated();

    if (reviewed != null) {
      final Instant update = sample.getUpdate();

      if (update.isAfter(reviewed)) {
        reviewed = update;
      }
    }

    return Sample.Builder.fromSample(sample)
        .withReviewed(reviewed)
        .withAttributes(attributes)
        .withExternalReferences(externalReferences)
        .withRelationships(relationships)
        .build();
  }

  Sample applyAllCurationToSample(Sample sample, final Optional<List<String>> curationDomains) {
    // short-circuit if no curation domains specified
    if (curationDomains.isPresent() && curationDomains.get().isEmpty()) {
      return sample;
    }

    // Try to apply curations in the order of creation date.
    // Because of the index in creation date mongo returns in that order
    final Set<CurationLink> curationLinks = new LinkedHashSet<>();
    int pageNo = 0;
    Page<CurationLink> page;

    do {
      final Pageable pageable = PageRequest.of(pageNo, 1000, Sort.Direction.ASC, "created");
      page = getCurationLinksForSample(sample.getAccession(), pageable);

      for (final CurationLink curationLink : page) {
        if (curationDomains.isPresent()) {
          // curation domains restricted, curation must be part of that domain
          if (curationDomains.get().contains(curationLink.getDomain())) {
            curationLinks.add(curationLink);
          }
        } else {
          // no curation domain restriction, use all
          curationLinks.add(curationLink);
        }
      }
      pageNo += 1;
    } while (pageNo < page.getTotalPages());

    for (final CurationLink curation : curationLinks) {
      try {
        sample = applyCurationLinkToSample(sample, curation);
      } catch (final IllegalArgumentException e) {
        log.trace(e.getMessage());
      }
    }

    if (sample.getReviewed() == null) {
      sample = Sample.Builder.fromSample(sample).withReviewed(sample.getUpdate()).build();
    }

    return sample;
  }
}
