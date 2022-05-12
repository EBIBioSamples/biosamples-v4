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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.hateoas.EntityModel;
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
    final String accession = sample.getAccession();
    final boolean hasOrganism =
        sample.getAttributes().stream()
            .anyMatch(attribute -> "organism".equals(attribute.getType().toLowerCase()));
    final boolean hasDerivedFrom =
        sample.getRelationships().stream()
            .anyMatch(
                relationship ->
                    "derived from".equals(relationship.getType().toLowerCase())
                        && accession.equals(relationship.getSource()));
    boolean success = true;

    if (!hasOrganism && hasDerivedFrom) {
      // walk up the derived from relationships and pull out all the organisms
      final Set<Attribute> organisms = getOrganismsForSample(sample, false);

      if (organisms.size() > 1) {
        // if there are multiple organisms, use a "mixed sample" taxonomy reference
        // some users expect one taxonomy reference, no more, no less
        LOG.debug("Applying curation to " + accession);
        applyCuration(mixedAttribute);
      } else if (organisms.size() == 1) {
        LOG.debug("Applying curation to " + accession);
        applyCuration(organisms.iterator().next());
      } else {
        success = false;
        failedQueue.add(accession);
        LOG.warn("Unable to find organism for " + accession);
      }
    } else if (hasOrganism && hasDerivedFrom) {
      // this sample has an organism, but that might have been applied by a previous curation
      for (final EntityModel<CurationLink> curationLink :
          bioSamplesClient.fetchCurationLinksOfSample(accession)) {
        if (domain.equals(curationLink.getContent().getDomain())) {
          SortedSet<Attribute> attributesPre =
              curationLink.getContent().getCuration().getAttributesPre();
          SortedSet<Attribute> attributesPost =
              curationLink.getContent().getCuration().getAttributesPost();
          // check that this is as structured as expected
          if (attributesPre.size() != 0) {
            throw new RuntimeException("Expected no pre attribute, got " + attributesPre.size());
          }
          if (attributesPost.size() != 1) {
            throw new RuntimeException(
                "Expected single post attribute, got " + attributesPost.size());
          }
          // this curation link was applied by us, check it is still valid
          final Set<Attribute> organisms = getOrganismsForSample(sample, true);

          if (organisms.size() > 1) {
            // check if the postattribute is the same as the organisms
            final String organism = "mixed sample";

            if (!organism.equals(attributesPost.iterator().next().getValue())) {
              LOG.debug("Replacing curation on " + accession + " with \"mixed Sample\"");

              bioSamplesClient.deleteCurationLink(curationLink.getContent());
              applyCuration(mixedAttribute);
            }
          } else if (organisms.size() == 1) {
            // check if the postattribute is the same as the organisms
            final Attribute organism = organisms.iterator().next();

            if (!organism.getValue().equals(attributesPost.iterator().next().getValue())) {
              LOG.debug("Replacing curation on " + accession + " with " + organism);

              bioSamplesClient.deleteCurationLink(curationLink.getContent());
              applyCuration(organism);
            }
          }
        }
      }
    }

    return new PipelineResult(sample.getAccession(), curationCount, success);
  }

  private void applyCuration(final Attribute organismValue) {
    final Set<Attribute> postAttributes = new HashSet<>();

    postAttributes.add(organismValue);
    final Curation curation = Curation.build(Collections.emptyList(), postAttributes);

    bioSamplesClient.persistCuration(sample.getAccession(), curation, domain, false);
    curationCount++;
  }

  private Set<Attribute> getOrganismsForSample(Sample sample, boolean ignoreSample) {
    final Set<Attribute> organisms = new HashSet<>();

    if (!ignoreSample) {
      for (Attribute attribute : sample.getAttributes()) {
        if ("organism".equals(attribute.getType().toLowerCase())) {
          organisms.add(attribute);
        }
      }
    }
    // if there are no organisms directly, check derived from relationships
    if (organisms.size() == 0) {
      LOG.trace("" + sample.getAccession() + " has no organism");

      for (final Relationship relationship : sample.getRelationships()) {
        if ("derived from".equals(relationship.getType().toLowerCase())
            && sample.getAccession().equals(relationship.getSource())) {
          LOG.trace("checking derived from " + relationship.getTarget());

          // recursion ahoy!
          bioSamplesClient
              .fetchSampleResource(relationship.getTarget())
              .ifPresent(
                  sampleResource ->
                      organisms.addAll(getOrganismsForSample(sampleResource.getContent(), false)));
        }
      }
    }

    return organisms;
  }
}
