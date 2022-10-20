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
package uk.ac.ebi.biosamples.copydown;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.biosamples.PipelineResult;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.*;

public class SampleCopydownCallable implements Callable<PipelineResult> {
  private static final Logger LOG = LoggerFactory.getLogger(SampleCopydownCallable.class);

  private static final Attribute mixedAttribute =
      Attribute.build(
          "organism", "mixed sample", "http://purl.obolibrary.org/obo/NCBITaxon_1427524", null);
  private final Sample sample;
  private final BioSamplesClient bioSamplesClient;
  private final String domain;
  private int curationCount;
  static final ConcurrentLinkedQueue<String> failedQueue = new ConcurrentLinkedQueue<>();

  public SampleCopydownCallable(
      final BioSamplesClient bioSamplesClient, final Sample sample, final String domain) {
    this.bioSamplesClient = bioSamplesClient;
    this.sample = sample;
    this.domain = domain;
    this.curationCount = 0;
  }

  @Override
  public PipelineResult call() {
    boolean success = true;
    final String accession = sample.getAccession();

    LOG.info("Handling sample for copy-down " + accession);

    try {
      final SortedSet<Attribute> attributes = sample.getAttributes();

      final boolean hasDerivedFrom =
          sample.getRelationships().stream()
              .anyMatch(
                  relationship ->
                      "derived from".equalsIgnoreCase(relationship.getType())
                          && accession.equals(relationship.getSource()));

      if (hasDerivedFrom) {
        final boolean hasOrganism =
            attributes.stream()
                .anyMatch(attribute -> "organism".equalsIgnoreCase(attribute.getType()));
        final Set<Attribute> attributesOfAllParentSamples = getAttributesOfParentSamples(sample);
        final Set<Attribute> qualifyingCopyDownAttributes =
            getQualifyingCopyDownAttributes(attributesOfAllParentSamples, attributes);

        if (hasOrganism) {
          // if child sample has organism, use it, don't copy down organism(s) from parent
          qualifyingCopyDownAttributes.removeIf(
              attribute -> attribute.getType().equalsIgnoreCase("organism"));
        } else {
          // get parent samples organisms
          final Set<Attribute> organisms = getOrganismsForSample(sample);

          if (organisms.size() > 1) {
            // if there are multiple organisms, use a "mixed sample" taxonomy reference
            // some users expect one taxonomy reference, no more, no less
            qualifyingCopyDownAttributes.removeIf(
                attribute -> attribute.getType().equalsIgnoreCase("organism"));
            qualifyingCopyDownAttributes.add(mixedAttribute);
          }
        }

        final int qualifyingCopyDownAttributesCount = qualifyingCopyDownAttributes.size();

        if (qualifyingCopyDownAttributesCount > 0) {
          LOG.info(
              "Adding "
                  + qualifyingCopyDownAttributesCount
                  + " copy-down curations for sample "
                  + accession);

          qualifyingCopyDownAttributes.forEach(this::applyCuration);
        } else {
          LOG.info("No copy-down curations for sample " + accession);
        }
      }
    } catch (final Exception e) {
      success = false;
    }

    return new PipelineResult(accession, curationCount, success);
  }

  private static Set<Attribute> getQualifyingCopyDownAttributes(
      Set<Attribute> attributesOfAllParentSamples,
      SortedSet<Attribute> attributesOfTheChildSample) {
    final Set<Attribute> qualifyingCopyDownAttributes = new TreeSet<>();

    attributesOfAllParentSamples.forEach(
        parentAttr -> {
          if (attributesOfTheChildSample.stream()
              .noneMatch(childAttr -> childAttr.getType().equalsIgnoreCase(parentAttr.getType()))) {
            qualifyingCopyDownAttributes.add(parentAttr);
          }
        });

    return qualifyingCopyDownAttributes;
  }

  private void applyCuration(final Attribute attribute) {
    final Set<Attribute> postAttributes = new HashSet<>();

    postAttributes.add(attribute);

    final Curation curation = Curation.build(Collections.emptyList(), postAttributes);

    bioSamplesClient.persistCuration(sample.getAccession(), curation, domain, false);
    curationCount++;
  }

  private Set<Attribute> getOrganismsForSample(final Sample sample) {
    final Set<Attribute> organisms = new HashSet<>();

    for (Attribute attribute : sample.getAttributes()) {
      if ("organism".equalsIgnoreCase(attribute.getType())) {
        organisms.add(attribute);
      }
    }

    // if there are no organisms directly, check derived from relationships
    if (organisms.size() == 0) {
      LOG.trace("" + sample.getAccession() + " has no organism");

      for (final Relationship relationship : sample.getRelationships()) {
        if ("derived from".equalsIgnoreCase(relationship.getType())
            && sample.getAccession().equals(relationship.getSource())) {
          LOG.trace("checking derived from " + relationship.getTarget());

          // recursion ahoy!
          bioSamplesClient
              .fetchSampleResource(relationship.getTarget())
              .ifPresent(
                  sampleResource ->
                      organisms.addAll(
                          getOrganismsForSample(
                              Objects.requireNonNull(sampleResource.getContent()))));
        }
      }
    }

    return organisms;
  }

  private Set<Attribute> getAttributesOfParentSamples(final Sample sample) {
    final Set<Attribute> parentSampleAttributes = new HashSet<>();

    for (final Relationship relationship : sample.getRelationships()) {
      if ("derived from".equalsIgnoreCase(relationship.getType())
          && sample.getAccession().equals(relationship.getSource())) {
        final String parentSample = relationship.getTarget();
        LOG.trace("checking derived from " + parentSample);

        bioSamplesClient
            .fetchSampleResource(parentSample)
            .ifPresent(
                sampleResource ->
                    parentSampleAttributes.addAll(
                        getAttributesOfParentSample(
                            Objects.requireNonNull(sampleResource.getContent()))));
      }
    }

    return parentSampleAttributes;
  }

  private Collection<? extends Attribute> getAttributesOfParentSample(final Sample sample) {
    return sample.getAttributes();
  }
}
