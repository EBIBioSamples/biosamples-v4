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
package uk.ac.ebi.biosamples.migration;

import com.google.common.collect.Sets;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.biosamples.model.*;

public class Comparer {

  private final Logger log = LoggerFactory.getLogger(getClass());

  public void compare(final String accession, final Sample oldSample, final Sample newSample) {

    if (!oldSample.getAccession().equals(newSample.getAccession())) {
      log.warn(
          "Difference on "
              + accession
              + " of accession between '"
              + oldSample.getAccession()
              + "' and '"
              + newSample.getAccession()
              + "'");
    }
    if (!oldSample.getName().equals(newSample.getName())) {
      log.warn(
          "Difference on "
              + accession
              + " of name between '"
              + oldSample.getName()
              + "' and '"
              + newSample.getName()
              + "'");
    }

    if (Math.abs(ChronoUnit.DAYS.between(oldSample.getUpdate(), newSample.getUpdate())) > 1) {
      log.warn(
          "Difference on "
              + accession
              + " of update date between '"
              + oldSample.getUpdate()
              + "' and '"
              + newSample.getUpdate()
              + "'");
    }
    if (Math.abs(ChronoUnit.DAYS.between(oldSample.getRelease(), newSample.getRelease())) > 1) {
      log.warn(
          "Difference on "
              + accession
              + " of release date between '"
              + oldSample.getRelease()
              + "' and '"
              + newSample.getRelease()
              + "'");
    }

    compareAttributes(oldSample, newSample);

    // relationships
    for (final Relationship relationship :
        Sets.difference(oldSample.getRelationships(), newSample.getRelationships())) {
      log.warn(
          "Difference on " + accession + " of relationship '" + relationship + "' in old only");
    }
    for (final Relationship relationship :
        Sets.difference(newSample.getRelationships(), oldSample.getRelationships())) {
      log.warn(
          "Difference on " + accession + " of relationship '" + relationship + "' in new only");
    }

    compareExternalReferences(oldSample, newSample);

    compareOrganizations(oldSample, newSample);

    compareContacts(oldSample, newSample);

    comparePublications(oldSample, newSample);
  }

  private void compareAttributes(final Sample oldSample, final Sample newSample) {
    final String accession = oldSample.getAccession();

    final Set<String> oldAttributeTypes =
        oldSample.getAttributes().stream().map(a -> a.getType()).collect(Collectors.toSet());
    final Set<String> newAttributeTypes =
        newSample.getAttributes().stream().map(a -> a.getType()).collect(Collectors.toSet());

    for (final String attributeType : Sets.difference(oldAttributeTypes, newAttributeTypes)) {
      log.warn("Difference on " + accession + " of attribute '" + attributeType + "' in old only");
    }

    for (final String attributeType : Sets.difference(newAttributeTypes, oldAttributeTypes)) {
      log.warn("Difference on " + accession + " of attribute '" + attributeType + "' in new only");
    }

    for (final String attributeType : Sets.intersection(oldAttributeTypes, newAttributeTypes)) {
      final List<Attribute> oldAttributes =
          oldSample.getAttributes().stream()
              .filter(a -> attributeType.equals(a.getType()))
              .collect(Collectors.toList());
      Collections.sort(oldAttributes);
      final List<Attribute> newAttributes =
          newSample.getAttributes().stream()
              .filter(a -> attributeType.equals(a.getType()))
              .collect(Collectors.toList());
      Collections.sort(newAttributes);

      final SortedMap<String, SortedMap<String, String>> oldUnits = new TreeMap<>();
      final SortedMap<String, SortedMap<String, SortedSet<String>>> oldIris = new TreeMap<>();
      final SortedMap<String, SortedMap<String, String>> newUnits = new TreeMap<>();
      final SortedMap<String, SortedMap<String, SortedSet<String>>> newIris = new TreeMap<>();

      if (oldAttributes.size() != newAttributes.size()) {
        log.warn(
            "Difference on "
                + accession
                + " of attribute '"
                + attributeType
                + "' has "
                + oldAttributes.size()
                + " values in old and "
                + newAttributes.size()
                + " values in new");
      } else {
        for (int i = 0; i < oldAttributes.size(); i++) {
          final Attribute oldAttribute = oldAttributes.get(i);
          final Attribute newAttribute = newAttributes.get(i);

          // TODO finish me

          if (!oldUnits.containsKey(oldAttribute.getType())) {
            oldUnits.put(oldAttribute.getType(), new TreeMap<>());
          }
          oldUnits.get(oldAttribute.getType()).put(oldAttribute.getValue(), oldAttribute.getUnit());

          if (!newUnits.containsKey(newAttribute.getType())) {
            newUnits.put(newAttribute.getType(), new TreeMap<>());
          }
          newUnits.get(newAttribute.getType()).put(newAttribute.getValue(), newAttribute.getUnit());

          if (!oldIris.containsKey(oldAttribute.getType())) {
            oldIris.put(oldAttribute.getType(), new TreeMap<>());
          }
          oldIris.get(oldAttribute.getType()).put(oldAttribute.getValue(), oldAttribute.getIri());

          if (!newIris.containsKey(newAttribute.getType())) {
            newIris.put(newAttribute.getType(), new TreeMap<>());
          }
          newIris.get(newAttribute.getType()).put(newAttribute.getValue(), newAttribute.getIri());

          // compare values
          if (!oldAttribute.getValue().equals(newAttribute.getValue())) {
            log.warn(
                "Difference on "
                    + accession
                    + " of attribute '"
                    + attributeType
                    + "' between '"
                    + oldAttribute.getValue()
                    + "' and '"
                    + newAttribute.getValue()
                    + "'");
          }

          // compare units
          if (oldAttribute.getUnit() != null
              && newAttribute.getUnit() != null
              && !oldAttribute.getUnit().equals(newAttribute.getUnit())) {
            log.warn(
                "Difference on "
                    + accession
                    + " of attribute '"
                    + attributeType
                    + "' between units '"
                    + oldAttribute.getUnit()
                    + "' and '"
                    + newAttribute.getUnit()
                    + "'");
          }
          // compare iris
          if (!oldAttribute.getIri().equals(newAttribute.getIri())) {
            if (oldAttribute.getIri().size() < newAttribute.getIri().size()) {
              log.warn(
                  "Difference on "
                      + accession
                      + " of attribute '"
                      + attributeType
                      + "' between iris '"
                      + oldAttribute.getIri()
                      + "' and '"
                      + newAttribute.getIri()
                      + "'");
            } else if (oldAttribute.getIri().size() > newAttribute.getIri().size()) {
              log.warn(
                  "Difference on "
                      + accession
                      + " of attribute '"
                      + attributeType
                      + "' between iris '"
                      + oldAttribute.getIri()
                      + "' and '"
                      + newAttribute.getIri()
                      + "'");
            } else {
              final Iterator<String> thisIt = oldAttribute.getIri().iterator();
              final Iterator<String> otherIt = newAttribute.getIri().iterator();
              while (thisIt.hasNext() && otherIt.hasNext()) {
                final int val = thisIt.next().compareTo(otherIt.next());
                if (val != 0) {
                  log.warn(
                      "Difference on "
                          + accession
                          + " of attribute '"
                          + attributeType
                          + "' between iris '"
                          + oldAttribute.getIri()
                          + "' and '"
                          + newAttribute.getIri()
                          + "'");
                }
              }
            }
          }
        }
      }
    }
  }

  private void compareOrganizations(final Sample oldSample, final Sample newSample) {
    final SortedSet<Organization> oldOrganizations = oldSample.getOrganizations();
    final SortedSet<Organization> newOrganizations = newSample.getOrganizations();

    for (final Organization oldOrganization : Sets.difference(oldOrganizations, newOrganizations)) {
      log.warn(
          "Difference on "
              + oldSample.getAccession()
              + " organization: only old sample has "
              + oldOrganization.toString());
    }

    for (final Organization newOrganization : Sets.difference(newOrganizations, oldOrganizations)) {
      log.warn(
          "Difference on "
              + oldSample.getAccession()
              + " organization: only new sample has "
              + newOrganization.toString());
    }
  }

  private void compareContacts(final Sample oldSample, final Sample newSample) {
    final SortedSet<Contact> oldContacts = oldSample.getContacts();
    final SortedSet<Contact> newContacts = newSample.getContacts();

    for (final Contact oldContact : Sets.difference(oldContacts, newContacts)) {
      log.warn(
          "Difference on "
              + oldSample.getAccession()
              + " contact: only old sample has "
              + oldContact.toString());
    }

    for (final Contact newContact : Sets.difference(newContacts, oldContacts)) {
      log.warn(
          "Difference on "
              + oldSample.getAccession()
              + " contact: only new sample has "
              + newContact.toString());
    }
  }

  private void comparePublications(final Sample oldSample, final Sample newSample) {
    final SortedSet<Publication> oldPublications = oldSample.getPublications();
    final SortedSet<Publication> newPublications = newSample.getPublications();

    for (final Publication oldPublication : Sets.difference(oldPublications, newPublications)) {
      log.warn(
          "Difference on "
              + oldSample.getAccession()
              + " pulication: only old sample has "
              + oldPublication.toString());
    }

    for (final Publication newPublication : Sets.difference(newPublications, oldPublications)) {
      log.warn(
          "Difference on "
              + oldSample.getAccession()
              + " pulication: only new sample has "
              + newPublication.toString());
    }
  }

  private void compareExternalReferences(final Sample oldSample, final Sample newSample) {
    final SortedSet<ExternalReference> oldExternalReferences = oldSample.getExternalReferences();
    final SortedSet<ExternalReference> newExternalReferences = newSample.getExternalReferences();

    for (final ExternalReference oldExternalReference :
        Sets.difference(oldExternalReferences, newExternalReferences)) {
      log.warn(
          "Difference on "
              + oldSample.getAccession()
              + " external reference: only old sample has "
              + oldExternalReference.toString());
    }

    for (final ExternalReference newExternalReference :
        Sets.difference(newExternalReferences, oldExternalReferences)) {
      log.warn(
          "Difference on "
              + oldSample.getAccession()
              + " external reference: only new sample has "
              + newExternalReference.toString());
    }
  }
}
