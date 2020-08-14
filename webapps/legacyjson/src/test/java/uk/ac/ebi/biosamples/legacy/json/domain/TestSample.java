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
package uk.ac.ebi.biosamples.legacy.json.domain;

import java.time.Instant;
import java.util.SortedSet;
import java.util.TreeSet;
import uk.ac.ebi.biosamples.model.*;

public class TestSample {

  private final String accession;
  private String name;
  private Instant releaseDate;
  private SortedSet<Attribute> attributes;
  private SortedSet<Relationship> relationships;
  private SortedSet<ExternalReference> externalReferences;
  private final String testDomain = "testDomain";
  private SortedSet<Organization> organizations;
  private SortedSet<Contact> contacts;
  private SortedSet<Publication> publications;

  public TestSample(String accession) {
    this.accession = accession;
    this.name = "test";
    this.attributes = new TreeSet<>();
    this.relationships = new TreeSet<>();
    this.externalReferences = new TreeSet<>();
    this.organizations = new TreeSet<>();
    this.contacts = new TreeSet<>();
    this.publications = new TreeSet<>();
    this.releaseDate = Instant.now();
  }

  public TestSample withName(String name) {
    this.name = name;
    return this;
  }

  public TestSample withAttribute(Attribute attribute) {
    this.attributes.add(attribute);
    return this;
  }

  public TestSample withRelationship(Relationship rel) {
    this.relationships.add(rel);
    return this;
  }

  public TestSample releasedOn(Instant releaseDate) {
    this.releaseDate = releaseDate;
    return this;
  }

  public TestSample withExternalReference(ExternalReference ref) {
    this.externalReferences.add(ref);
    return this;
  }

  public TestSample withOrganization(Organization org) {
    this.organizations.add(org);
    return this;
  }

  public TestSample withContact(Contact contact) {
    this.contacts.add(contact);
    return this;
  }

  public TestSample withPublication(Publication publication) {
    this.publications.add(publication);
    return this;
  }

  public Sample build() {
    return new Sample.Builder(this.name, this.accession)
        .withDomain(testDomain)
        .withRelease(this.releaseDate)
        .withUpdate(Instant.now())
        .withAttributes(attributes)
        .withRelationships(relationships)
        .withExternalReferences(externalReferences)
        .withOrganizations(organizations)
        .withContacts(contacts)
        .withPublications(publications)
        .build();
  }
}
