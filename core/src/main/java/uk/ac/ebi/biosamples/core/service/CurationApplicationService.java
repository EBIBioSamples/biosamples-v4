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
package uk.ac.ebi.biosamples.core.service;

import java.util.Collection;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.core.model.Attribute;
import uk.ac.ebi.biosamples.core.model.Curation;
import uk.ac.ebi.biosamples.core.model.ExternalReference;
import uk.ac.ebi.biosamples.core.model.Sample;

@Service
public class CurationApplicationService {
  private final Logger log = LoggerFactory.getLogger(getClass());

  public Sample applyCurationToSample(final Sample sample, final Curation curation) {
    log.trace("Applying curation " + curation + " to sample " + sample);

    final SortedSet<Attribute> attributes = new TreeSet<>(sample.getAttributes());
    final SortedSet<ExternalReference> externalReferences =
        new TreeSet<>(sample.getExternalReferences());

    // remove pre-curation things
    for (final Attribute attribute : curation.getAttributesPre()) {
      if (!attributes.contains(attribute)) {
        throw new IllegalArgumentException(
            "Attempting to apply curation " + curation + " to sample " + sample);
      }

      attributes.remove(attribute);
    }
    for (final ExternalReference externalReference : curation.getExternalReferencesPre()) {
      if (!externalReferences.contains(externalReference)) {
        throw new IllegalArgumentException(
            "Attempting to apply curation " + curation + " to sample " + sample);
      }

      externalReferences.remove(externalReference);
    }
    // add post-curation things
    for (final Attribute attribute : curation.getAttributesPost()) {
      if (attributes.contains(attribute)) {
        throw new IllegalArgumentException(
            "Attempting to apply curation " + curation + " to sample " + sample);
      }

      attributes.add(attribute);
    }
    for (final ExternalReference externalReference : curation.getExternalReferencesPost()) {
      if (externalReferences.contains(externalReference)) {
        throw new IllegalArgumentException(
            "Attempting to apply curation " + curation + " to sample " + sample);
      }

      externalReferences.add(externalReference);
    }

    return Sample.build(
        sample.getName(),
        sample.getAccession(),
        sample.getSraAccession(),
        sample.getDomain(),
        sample.getWebinSubmissionAccountId(),
        sample.getTaxId(),
        sample.getStatus(),
        sample.getRelease(),
        sample.getUpdate(),
        sample.getCreate(),
        sample.getSubmitted(),
        sample.getReviewed(),
        attributes,
        sample.getData(),
        sample.getStructuredData(),
        sample.getRelationships(),
        externalReferences,
        sample.getOrganizations(),
        sample.getContacts(),
        sample.getPublications(),
        sample.getCertificates(),
        sample.getSubmittedVia());
  }

  public Sample applyAllCurationToSample(Sample sample, final Collection<Curation> curations) {
    boolean curationApplied = true;

    while (curationApplied && !curations.isEmpty()) {
      final Iterator<Curation> it = curations.iterator();
      curationApplied = false;

      while (it.hasNext()) {
        final Curation curation = it.next();

        try {
          sample = applyCurationToSample(sample, curation);
          it.remove();
          curationApplied = true;
        } catch (final IllegalArgumentException e) {
          // do nothing, will try again next loop
        }
      }
    }

    return sample;
  }
}
