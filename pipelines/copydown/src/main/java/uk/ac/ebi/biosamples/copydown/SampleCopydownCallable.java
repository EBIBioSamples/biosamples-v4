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
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.biosamples.PipelineResult;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.*;

public class SampleCopydownCallable implements Callable<PipelineResult> {
  private static final Logger LOG = LoggerFactory.getLogger(SampleCopydownCallable.class);
  private static final String ORGANISM = "organism";
  private static final Attribute mixedAttribute =
      Attribute.build(
          ORGANISM, "mixed sample", "http://purl.obolibrary.org/obo/NCBITaxon_1427524", null);
  private static final String DERIVED_FROM = "derived from";
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
    curationCount = 0;
  }

  @Override
  public PipelineResult call() {
    boolean success = true;
    final String accession = sample.getAccession();

    LOG.info("Handling sample for copy-down " + accession);

    try {
      final SortedSet<Attribute> attributes = sample.getAttributes();

      if (hasDerivedFromRelationship()) {
        processDerivedFromAttributes(attributes);
      }
    } catch (final Exception e) {
      success = false;
    }

    return new PipelineResult(accession, curationCount, success);
  }

  private boolean hasDerivedFromRelationship() {
    return sample.getRelationships().stream()
        .anyMatch(
            relationship ->
                DERIVED_FROM.equalsIgnoreCase(relationship.getType())
                    && sample.getAccession().equals(relationship.getSource()));
  }

  private void processDerivedFromAttributes(final SortedSet<Attribute> childAttributes) {
    final Map<String, Set<Attribute>> sampleAccessionToAttributeMap =
        getAttributesOfParentSamples(sample);
    final Set<Attribute> qualifyingCopyDownAttributes =
        getQualifyingCopyDownAttributes(sampleAccessionToAttributeMap, childAttributes);

    qualifyingCopyDownAttributes.removeIf(
        attribute -> ORGANISM.equalsIgnoreCase(attribute.getType()));
    applyCopyDownCuration(qualifyingCopyDownAttributes);
  }

  private Set<Attribute> getQualifyingCopyDownAttributes(
      final Map<String, Set<Attribute>> sampleAccessionToAttributeMap,
      final SortedSet<Attribute> attributesOfTheChildSample) {
    final Set<Attribute> qualifyingCopyDownAttributes = new TreeSet<>();
    final Set<Attribute> uniqueParentSamplesAttributes = mergeSets(sampleAccessionToAttributeMap);

    uniqueParentSamplesAttributes.forEach(
        parentAttr -> {
          if (attributesOfTheChildSample.stream()
              .noneMatch(childAttr -> childAttr.getType().equalsIgnoreCase(parentAttr.getType()))) {
            qualifyingCopyDownAttributes.add(parentAttr);
          }
        });

    return qualifyingCopyDownAttributes;
  }

  public static Set<Attribute> mergeSets(final Map<String, Set<Attribute>> map) {
    final Set<Attribute> mergedSet = new TreeSet<>(); // Using TreeSet to keep elements sorted
    final Set<Attribute> uniqueElements = new HashSet<>(); // Elements appearing in only one set

    for (final Set<Attribute> set : map.values()) {
      final Set<String> duplicateCheck = new HashSet<>();

      for (final Attribute element : set) {
        // Add to uniqueElements if it hasn't been added before
        if (!duplicateCheck.contains(element.getType()) && !uniqueElements.contains(element)) {
          uniqueElements.add(element);
        } else {
          // If element already exists in uniqueElements, remove it
          uniqueElements.remove(element);
          // Add to duplicateCheck to avoid adding it again
          duplicateCheck.add(element.getType());
        }
      }
    }

    mergedSet.addAll(uniqueElements);

    return mergedSet;
  }

  private void applyCopyDownCuration(final Set<Attribute> attributes) {
    if (!attributes.isEmpty()) {
      LOG.info(
          "Adding "
              + attributes.size()
              + " copy-down curations for sample "
              + sample.getAccession());

      attributes.forEach(this::applyCuration);
    } else {
      LOG.info("No copy-down curations for sample " + sample.getAccession());
    }
  }

  private void applyCuration(final Attribute attribute) {
    final Set<Attribute> postAttributes = new HashSet<>();

    postAttributes.add(attribute);

    final Curation curation = Curation.build(Collections.emptyList(), postAttributes);

    bioSamplesClient.persistCuration(sample.getAccession(), curation, domain, false);
    curationCount++;
  }

  private Map<String, Set<Attribute>> getAttributesOfParentSamples(final Sample sample) {
    final Map<String, Set<Attribute>> sampleAccessionToAttributeMap = new HashMap<>();

    for (final Relationship relationship : sample.getRelationships()) {
      if (DERIVED_FROM.equalsIgnoreCase(relationship.getType())
          && sample.getAccession().equals(relationship.getSource())) {
        final String parentSample = relationship.getTarget();

        LOG.trace("checking derived from " + parentSample);

        bioSamplesClient
            .fetchSampleResource(parentSample)
            .ifPresent(
                sampleResource -> {
                  final Sample parent = sampleResource.getContent();

                  if (parent != null) {
                    if (parent.getSubmittedVia() == SubmittedViaType.PIPELINE_IMPORT) {
                      sampleAccessionToAttributeMap.put(
                          parent.getAccession(),
                          parent.getAttributes().stream()
                              .filter(
                                  attribute ->
                                      attribute.getTag().equals("attribute")
                                          && isAttributeEligibleForCopydown(attribute.getType()))
                              .collect(Collectors.toSet()));
                    } else {
                      sampleAccessionToAttributeMap.put(
                          parent.getAccession(),
                          parent.getAttributes().stream()
                              .filter(
                                  attribute ->
                                      !attribute.getType().startsWith("ENA")
                                          && isAttributeEligibleForCopydown(attribute.getType()))
                              .collect(Collectors.toSet()));
                    }
                  }
                });
      }
    }

    return sampleAccessionToAttributeMap;
  }

  private boolean isAttributeEligibleForCopydown(final String type) {
    return !type.startsWith("SRA accession")
        && !type.startsWith("broker name")
        && !type.startsWith("INSDC")
        && !type.startsWith("title")
        && !type.startsWith("description")
        && !type.startsWith("Submitter Id")
        && !type.startsWith("Secondary Id")
        && !type.startsWith("organism")
        && !type.startsWith("uuid")
        && !type.startsWith("individual_name")
        && !type.startsWith("anonymized_name")
        && !type.startsWith("common name")
        && !type.startsWith("ENA");
  }
}
