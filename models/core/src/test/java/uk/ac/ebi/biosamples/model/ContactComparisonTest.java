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
package uk.ac.ebi.biosamples.model;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class ContactComparisonTest {

  @Test
  public void testComparisonOfNames() {
    Contact firstContact = new Contact.Builder().firstName("Arnold").lastName("Pigeon").build();

    Contact secondContact = new Contact.Builder().firstName("Arnold").lastName("Bigeon").build();

    Assert.assertEquals(
        firstContact.compareTo(secondContact), oldComparison(firstContact, secondContact));
  }

  @Test
  public void testGeneral() {
    Contact firstContact =
        new Contact.Builder()
            .firstName("Timmy")
            .midInitials("TT")
            .lastName("Tommy")
            .email("tt@ttcorp.com")
            .role("eater")
            .affiliation("TTCorp")
            .url("ttcorp.com")
            .build();

    Contact secondContact =
        new Contact.Builder()
            .firstName("Timmy")
            .midInitials("TT")
            .lastName("Tommy")
            .email("tu@ttcorp.com")
            .role("eater")
            .affiliation("TTCorp")
            .url("ttcorp.com")
            .build();

    Contact thirdContact =
            new Contact.Builder()
                    .email("tt@ttcorp.com")
                    .build();

    Contact fourthContact =
            new Contact.Builder()
                    .email("tu@ttcorp.com")
                    .build();

    Assert.assertEquals(
        firstContact.compareTo(secondContact), oldComparison(firstContact, secondContact));
    Assert.assertEquals(firstContact.compareTo(secondContact), -1);

    Assert.assertEquals(
            thirdContact.compareTo(fourthContact), oldComparison(thirdContact, fourthContact));
  }

  private int oldComparison(Contact first, Contact second) {
    if (first.getFirstName() == null && second.getFirstName() != null) {
      return -1;
    }
    if (first.getFirstName() != null && second.getFirstName() == null) {
      return 1;
    }
    if (first.getFirstName() != null
        && second.getFirstName() != null
        && !first.getFirstName().equals(second.getFirstName())) {
      return first.getFirstName().compareTo(second.getFirstName());
    }

    if (first.getLastName() == null && second.getLastName() != null) {
      return -1;
    }
    if (first.getLastName() != null && second.getLastName() == null) {
      return 1;
    }
    if (first.getLastName() != null
        && second.getLastName() != null
        && !first.getLastName().equals(second.getLastName())) {
      return first.getLastName().compareTo(second.getLastName());
    }

    if (first.getMidInitials() == null && second.getMidInitials() != null) {
      return -1;
    }
    if (first.getMidInitials() != null && second.getMidInitials() == null) {
      return 1;
    }
    if (first.getMidInitials() != null
        && second.getMidInitials() != null
        && !first.getMidInitials().equals(second.getMidInitials())) {
      return first.getMidInitials().compareTo(second.getMidInitials());
    }

    if (first.getRole() == null && second.getRole() != null) {
      return -1;
    }
    if (first.getRole() != null && second.getRole() == null) {
      return 1;
    }
    if (first.getRole() != null
        && second.getRole() != null
        && !first.getRole().equals(second.getRole())) {
      return first.getRole().compareTo(second.getRole());
    }

    if (first.getEmail() == null && second.getEmail() != null) {
      return -1;
    }
    if (first.getEmail() != null && second.getEmail() == null) {
      return 1;
    }
    if (first.getEmail() != null
        && second.getEmail() != null
        && !first.getEmail().equals(second.getEmail())) {
      return first.getEmail().compareTo(second.getEmail());
    }

    if (first.getAffiliation() == null && second.getAffiliation() != null) {
      return -1;
    }
    if (first.getAffiliation() != null && second.getAffiliation() == null) {
      return 1;
    }
    if (first.getAffiliation() != null
        && second.getAffiliation() != null
        && !first.getAffiliation().equals(second.getAffiliation())) {
      return first.getAffiliation().compareTo(second.getAffiliation());
    }

    if (first.getUrl() == null && second.getUrl() != null) {
      return -1;
    }
    if (first.getUrl() != null && second.getUrl() == null) {
      return 1;
    }
    if (first.getUrl() != null
        && second.getUrl() != null
        && !first.getUrl().equals(second.getUrl())) {
      return first.getUrl().compareTo(second.getUrl());
    }
    // no differences, must be the same
    return 0;
  }
}
