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

import com.google.common.base.Strings;
import java.time.Instant;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.model.Contact;
import uk.ac.ebi.biosamples.model.Sample;

@Service
public class SampleManipulationService {

  /**
   * Remove the legacy fields from the contacts in the sample
   *
   * @param sample
   * @return a sample which contacts has only name field
   */
  public Sample removeLegacyFields(final Sample sample) {
    final SortedSet<Contact> contacts =
        sample.getContacts().stream()
            .map(this::removeContactLegacyFields)
            .collect(Collectors.toCollection(TreeSet::new));

    return Sample.Builder.fromSample(sample).withContacts(contacts).build();
  }

  /**
   * Reset the sample update date to now
   *
   * @param sample
   * @return sample which update date is set to current instant
   */
  public Sample setUpdateDateToNow(final Sample sample) {
    //        return Sample.build(sample.getName(), sample.getAccession(), sample.getDomain(),
    //                sample.getRelease(), Instant.now(),
    //                sample.getCharacteristics(), sample.getRelationships(),
    // sample.getExternalReferences(),
    //                sample.getOrganizations(), sample.getContacts(),
    // sample.getPublications());
    return Sample.Builder.fromSample(sample).withUpdate(Instant.now()).build();
  }

  /**
   * Maintain the name from the contact as unique field
   *
   * @param contact
   * @return the polished contact
   */
  private Contact removeContactLegacyFields(final Contact contact) {
    String name = contact.getName();
    if (Strings.isNullOrEmpty(name)) {
      final String nullSafeFirstName = contact.getFirstName() == null ? "" : contact.getFirstName();
      final String nullSafeLastName = contact.getLastName() == null ? "" : contact.getLastName();
      final String fullName = (nullSafeFirstName + " " + nullSafeLastName).trim();

      if (!fullName.isEmpty()) {
        name = fullName;
      }
    }
    return new Contact.Builder().name(name).build();
  }
}
