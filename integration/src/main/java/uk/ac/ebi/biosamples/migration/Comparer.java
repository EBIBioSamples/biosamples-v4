/*
* Copyright 2019 EMBL - European Bioinformatics Institute
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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.Contact;
import uk.ac.ebi.biosamples.model.ExternalReference;
import uk.ac.ebi.biosamples.model.Organization;
import uk.ac.ebi.biosamples.model.Publication;
import uk.ac.ebi.biosamples.model.Relationship;
import uk.ac.ebi.biosamples.model.Sample;

public class Comparer {

  private final Logger log = LoggerFactory.getLogger(getClass());

  public void compare(String accession, Sample oldSample, Sample newSample) {

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
    for (Relationship relationship :
        Sets.difference(oldSample.getRelationships(), newSample.getRelationships())) {
      log.warn(
          "Difference on " + accession + " of relationship '" + relationship + "' in old only");
    }
    for (Relationship relationship :
        Sets.difference(newSample.getRelationships(), oldSample.getRelationships())) {
      log.warn(
          "Difference on " + accession + " of relationship '" + relationship + "' in new only");
    }

    compareExternalReferences(oldSample, newSample);

    compareOrganizations(oldSample, newSample);

    compareContacts(oldSample, newSample);

    comparePublications(oldSample, newSample);
  }

  private void compareAttributes(Sample oldSample, Sample newSample) {
    String accession = oldSample.getAccession();

    Set<String> oldAttributeTypes =
        oldSample.getAttributes().stream().map(a -> a.getType()).collect(Collectors.toSet());
    Set<String> newAttributeTypes =
        newSample.getAttributes().stream().map(a -> a.getType()).collect(Collectors.toSet());

    for (String attributeType : Sets.difference(oldAttributeTypes, newAttributeTypes)) {
      log.warn("Difference on " + accession + " of attribute '" + attributeType + "' in old only");
    }

    for (String attributeType : Sets.difference(newAttributeTypes, oldAttributeTypes)) {
      log.warn("Difference on " + accession + " of attribute '" + attributeType + "' in new only");
    }

    for (String attributeType : Sets.intersection(oldAttributeTypes, newAttributeTypes)) {
      List<Attribute> oldAttributes =
          oldSample.getAttributes().stream()
              .filter(a -> attributeType.equals(a.getType()))
              .collect(Collectors.toList());
      Collections.sort(oldAttributes);
      List<Attribute> newAttributes =
          newSample.getAttributes().stream()
              .filter(a -> attributeType.equals(a.getType()))
              .collect(Collectors.toList());
      Collections.sort(newAttributes);

      SortedMap<String, SortedMap<String, String>> oldUnits = new TreeMap<>();
      SortedMap<String, SortedMap<String, SortedSet<String>>> oldIris = new TreeMap<>();
      SortedMap<String, SortedMap<String, String>> newUnits = new TreeMap<>();
      SortedMap<String, SortedMap<String, SortedSet<String>>> newIris = new TreeMap<>();

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
          Attribute oldAttribute = oldAttributes.get(i);
          Attribute newAttribute = newAttributes.get(i);

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
              Iterator<String> thisIt = oldAttribute.getIri().iterator();
              Iterator<String> otherIt = newAttribute.getIri().iterator();
              while (thisIt.hasNext() && otherIt.hasNext()) {
                int val = thisIt.next().compareTo(otherIt.next());
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

  private void compareOrganizations(Sample oldSample, Sample newSample) {
    SortedSet<Organization> oldOrganizations = oldSample.getOrganizations();
    SortedSet<Organization> newOrganizations = newSample.getOrganizations();

    for (Organization oldOrganization : Sets.difference(oldOrganizations, newOrganizations)) {
      log.warn(
          "Difference on "
              + oldSample.getAccession()
              + " organization: only old sample has "
              + oldOrganization.toString());
    }

    for (Organization newOrganization : Sets.difference(newOrganizations, oldOrganizations)) {
      log.warn(
          "Difference on "
              + oldSample.getAccession()
              + " organization: only new sample has "
              + newOrganization.toString());
    }
  }

  private void compareContacts(Sample oldSample, Sample newSample) {
    SortedSet<Contact> oldContacts = oldSample.getContacts();
    SortedSet<Contact> newContacts = newSample.getContacts();

    for (Contact oldContact : Sets.difference(oldContacts, newContacts)) {
      log.warn(
          "Difference on "
              + oldSample.getAccession()
              + " contact: only old sample has "
              + oldContact.toString());
    }

    for (Contact newContact : Sets.difference(newContacts, oldContacts)) {
      log.warn(
          "Difference on "
              + oldSample.getAccession()
              + " contact: only new sample has "
              + newContact.toString());
    }
  }

  private void comparePublications(Sample oldSample, Sample newSample) {
    SortedSet<Publication> oldPublications = oldSample.getPublications();
    SortedSet<Publication> newPublications = newSample.getPublications();

    for (Publication oldPublication : Sets.difference(oldPublications, newPublications)) {
      log.warn(
          "Difference on "
              + oldSample.getAccession()
              + " pulication: only old sample has "
              + oldPublication.toString());
    }

    for (Publication newPublication : Sets.difference(newPublications, oldPublications)) {
      log.warn(
          "Difference on "
              + oldSample.getAccession()
              + " pulication: only new sample has "
              + newPublication.toString());
    }
  }

  private void compareExternalReferences(Sample oldSample, Sample newSample) {
    SortedSet<ExternalReference> oldExternalReferences = oldSample.getExternalReferences();
    SortedSet<ExternalReference> newExternalReferences = newSample.getExternalReferences();

    for (ExternalReference oldExternalReference :
        Sets.difference(oldExternalReferences, newExternalReferences)) {
      log.warn(
          "Difference on "
              + oldSample.getAccession()
              + " external reference: only old sample has "
              + oldExternalReference.toString());
    }

    for (ExternalReference newExternalReference :
        Sets.difference(newExternalReferences, oldExternalReferences)) {
      log.warn(
          "Difference on "
              + oldSample.getAccession()
              + " external reference: only new sample has "
              + newExternalReference.toString());
    }
  }
}
