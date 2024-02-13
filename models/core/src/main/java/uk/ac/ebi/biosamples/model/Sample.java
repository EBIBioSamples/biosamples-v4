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
package uk.ac.ebi.biosamples.model;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;
import static java.time.format.DateTimeFormatter.ISO_LOCAL_TIME;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.TemporalAccessor;
import java.util.*;
import lombok.Getter;
import lombok.ToString;
import uk.ac.ebi.biosamples.model.structured.AbstractData;
import uk.ac.ebi.biosamples.model.structured.StructuredDataTable;
import uk.ac.ebi.biosamples.service.CharacteristicDeserializer;
import uk.ac.ebi.biosamples.service.CharacteristicSerializer;
import uk.ac.ebi.biosamples.service.CustomInstantDeserializer;
import uk.ac.ebi.biosamples.service.CustomInstantSerializer;

@Getter
@ToString
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonPropertyOrder({
  "name",
  "accession",
  "sraAccession",
  "domain",
  "webinSubmissionAccountId",
  "taxId",
  "status",
  "release",
  "update",
  "submitted",
  "characteristics",
  "relationships",
  "externalReferences",
  "releaseDate",
  "updateDate",
  "submittedDate",
  "submittedVia"
})
public class Sample implements Comparable<Sample> {
  @JsonProperty("accession")
  protected String accession;

  @JsonProperty("sraAccession")
  protected String sraAccession;

  @JsonProperty("name")
  protected String name;

  @JsonProperty("domain")
  protected String domain;

  @JsonProperty("webinSubmissionAccountId")
  protected String webinSubmissionAccountId;

  protected Long taxId;
  protected SampleStatus status;

  @JsonSerialize(using = CustomInstantSerializer.class)
  protected Instant release;

  @JsonSerialize(using = CustomInstantSerializer.class)
  protected Instant update;

  @JsonSerialize(using = CustomInstantSerializer.class)
  protected Instant create;

  @JsonSerialize(using = CustomInstantSerializer.class)
  protected Instant submitted;

  @JsonSerialize(using = CustomInstantSerializer.class)
  @JsonIgnore
  protected Instant reviewed;

  @JsonIgnore protected SortedSet<Attribute> attributes;

  @JsonProperty("data")
  protected SortedSet<AbstractData> data;

  @JsonProperty("structuredData")
  protected Set<StructuredDataTable> structuredData;

  @JsonProperty("relationships")
  protected SortedSet<Relationship> relationships;

  @JsonProperty("externalReferences")
  protected SortedSet<ExternalReference> externalReferences;

  @JsonProperty("organization")
  protected SortedSet<Organization> organizations;

  @JsonProperty("contact")
  protected SortedSet<Contact> contacts;

  @JsonProperty("publications")
  protected SortedSet<Publication> publications;

  @JsonProperty("certificates")
  protected SortedSet<Certificate> certificates;

  @JsonProperty("submittedVia")
  private SubmittedViaType submittedVia;

  protected Sample() {}

  @JsonSerialize(using = CustomInstantSerializer.class)
  @JsonIgnore
  public Instant getReviewed() {
    return reviewed;
  }

  @JsonIgnore
  public boolean hasAccession() {
    return accession != null && !accession.trim().isEmpty();
  }

  @JsonIgnore
  public boolean hasSraAccession() {
    return sraAccession != null && !sraAccession.trim().isEmpty();
  }

  @JsonProperty(value = "releaseDate", access = JsonProperty.Access.READ_ONLY)
  @JsonIgnore
  public String getReleaseDate() {
    return release != null
        ? ZonedDateTime.ofInstant(release, ZoneOffset.UTC).format(ISO_LOCAL_DATE)
        : null;
  }

  @JsonProperty(value = "updateDate", access = JsonProperty.Access.READ_ONLY)
  @JsonIgnore
  public String getUpdateDate() {
    return update != null
        ? ZonedDateTime.ofInstant(update, ZoneOffset.UTC).format(ISO_LOCAL_DATE)
        : null;
  }

  @JsonProperty(value = "submittedDate", access = JsonProperty.Access.READ_ONLY)
  @JsonIgnore
  public String getSubmittedDate() {
    return submitted != null
        ? ZonedDateTime.ofInstant(submitted, ZoneOffset.UTC).format(ISO_LOCAL_DATE)
        : null;
  }

  @JsonProperty(value = "reviewedDate", access = JsonProperty.Access.READ_ONLY)
  @JsonIgnore
  public String getReviewedDate() {
    return reviewed != null
        ? ZonedDateTime.ofInstant(reviewed, ZoneOffset.UTC).format(ISO_LOCAL_DATE)
        : null;
  }

  @JsonProperty(value = "taxId")
  public Long getTaxId() {
    if (taxId != null) {
      return taxId;
    }

    Optional<Long> taxon = Optional.empty();

    for (final Attribute attribute : attributes) {
      if ("organism".equalsIgnoreCase(attribute.getType()) && !attribute.getIri().isEmpty()) {
        taxon =
            attribute.getIri().stream()
                .map(this::extractTaxIdFromIri)
                .filter(i -> i > 0)
                .findFirst();
        break;
      }
    }

    return taxon.orElse(null);
  }

  @JsonProperty(value = "status")
  public SampleStatus getStatus() {
    if (status != null) {
      return status;
    }

    return null;
  }

  private long extractTaxIdFromIri(final String iri) {
    if (iri.isEmpty()) {
      return 0;
    }
    final String[] segments = iri.split("NCBITaxon_");
    try {
      return Integer.parseInt(segments[segments.length - 1]);
    } catch (final NumberFormatException e) {
      return 0;
    }
  }

  // DO NOT specify the JSON property value manually, must be autoinferred or errors
  @JsonSerialize(using = CharacteristicSerializer.class)
  public SortedSet<Attribute> getCharacteristics() {
    return attributes;
  }

  @Override
  public boolean equals(final Object o) {
    if (o == this) {
      return true;
    }

    if (!(o instanceof Sample)) {
      return false;
    }

    final Sample other = (Sample) o;

    // dont use update date for comparisons, too volatile. SubmittedVia doesnt contain information
    // for comparison

    return Objects.equals(name, other.name)
        && Objects.equals(accession, other.accession)
        && Objects.equals(domain, other.domain)
        && Objects.equals(webinSubmissionAccountId, other.webinSubmissionAccountId)
        && Objects.equals(taxId, other.taxId)
        && Objects.equals(status, other.status)
        && Objects.equals(release, other.release)
        && Objects.equals(attributes, other.attributes)
        && Objects.equals(data, other.data)
        && Objects.equals(relationships, other.relationships)
        && Objects.equals(externalReferences, other.externalReferences)
        && Objects.equals(organizations, other.organizations)
        && Objects.equals(contacts, other.contacts)
        && Objects.equals(publications, other.publications);
  }

  @Override
  public int hashCode() {
    // don't put update date in the hash because it's not in comparison
    return Objects.hash(
        name,
        accession,
        taxId,
        status,
        release,
        attributes,
        data,
        relationships,
        externalReferences,
        organizations,
        publications);
  }

  @Override
  public int compareTo(final Sample other) {
    if (other == null) {
      return 1;
    }

    if (!accession.equals(other.accession)) {
      return accession.compareTo(other.accession);
    }

    if (!sraAccession.equals(other.sraAccession)) {
      return sraAccession.compareTo(other.sraAccession);
    }

    if (!name.equals(other.name)) {
      return name.compareTo(other.name);
    }

    if (!taxId.equals(other.taxId)) {
      return taxId.compareTo(other.taxId);
    }

    if (!status.equals(other.status)) {
      return status.compareTo(other.status);
    }

    if (!release.equals(other.release)) {
      return release.compareTo(other.release);
    }

    if (!attributes.equals(other.attributes)) {
      if (attributes.size() < other.attributes.size()) {
        return -1;
      } else if (attributes.size() > other.attributes.size()) {
        return 1;
      } else {
        final Iterator<Attribute> thisIt = attributes.iterator();
        final Iterator<Attribute> otherIt = other.attributes.iterator();
        while (thisIt.hasNext() && otherIt.hasNext()) {
          final int val = thisIt.next().compareTo(otherIt.next());
          if (val != 0) {
            return val;
          }
        }
      }
    }
    if (!relationships.equals(other.relationships)) {
      if (relationships.size() < other.relationships.size()) {
        return -1;
      } else if (relationships.size() > other.relationships.size()) {
        return 1;
      } else {
        final Iterator<Relationship> thisIt = relationships.iterator();
        final Iterator<Relationship> otherIt = other.relationships.iterator();
        while (thisIt.hasNext() && otherIt.hasNext()) {
          final int val = thisIt.next().compareTo(otherIt.next());
          if (val != 0) {
            return val;
          }
        }
      }
    }
    if (!externalReferences.equals(other.externalReferences)) {
      if (externalReferences.size() < other.externalReferences.size()) {
        return -1;
      } else if (externalReferences.size() > other.externalReferences.size()) {
        return 1;
      } else {
        final Iterator<ExternalReference> thisIt = externalReferences.iterator();
        final Iterator<ExternalReference> otherIt = other.externalReferences.iterator();
        while (thisIt.hasNext() && otherIt.hasNext()) {
          final int val = thisIt.next().compareTo(otherIt.next());
          if (val != 0) {
            return val;
          }
        }
      }
    }
    if (!organizations.equals(other.organizations)) {
      if (organizations.size() < other.organizations.size()) {
        return -1;
      } else if (organizations.size() > other.organizations.size()) {
        return 1;
      } else {
        final Iterator<Organization> thisIt = organizations.iterator();
        final Iterator<Organization> otherIt = other.organizations.iterator();
        while (thisIt.hasNext() && otherIt.hasNext()) {
          final int val = thisIt.next().compareTo(otherIt.next());
          if (val != 0) {
            return val;
          }
        }
      }
    }
    if (!contacts.equals(other.contacts)) {
      if (contacts.size() < other.contacts.size()) {
        return -1;
      } else if (contacts.size() > other.contacts.size()) {
        return 1;
      } else {
        final Iterator<Contact> thisIt = contacts.iterator();
        final Iterator<Contact> otherIt = other.contacts.iterator();
        while (thisIt.hasNext() && otherIt.hasNext()) {
          final int val = thisIt.next().compareTo(otherIt.next());
          if (val != 0) {
            return val;
          }
        }
      }
    }
    if (!publications.equals(other.publications)) {
      if (publications.size() < other.publications.size()) {
        return -1;
      } else if (publications.size() > other.publications.size()) {
        return 1;
      } else {
        final Iterator<Publication> thisIt = publications.iterator();
        final Iterator<Publication> otherIt = other.publications.iterator();
        while (thisIt.hasNext() && otherIt.hasNext()) {
          final int val = thisIt.next().compareTo(otherIt.next());
          if (val != 0) {
            return val;
          }
        }
      }
    }
    return 0;
  }

  public static Sample build(
      final String name,
      final String accession,
      final String sraAccession,
      final String domain,
      final String webinSubmissionAccountId,
      final Long taxId,
      final SampleStatus status,
      final Instant release,
      final Instant update,
      final Instant create,
      final Instant submitted,
      final Instant reviewed,
      final Set<Attribute> attributes,
      final Set<Relationship> relationships,
      final Set<ExternalReference> externalReferences) {
    return build(
        name,
        accession,
        sraAccession,
        domain,
        webinSubmissionAccountId,
        taxId,
        status,
        release,
        update,
        create,
        submitted,
        reviewed,
        attributes,
        null,
        null,
        relationships,
        externalReferences,
        null,
        null,
        null,
        null,
        null);
  }

  public static Sample build(
      final String name,
      final String accession,
      final String sraAccession,
      final String domain,
      final String webinSubmissionAccountId,
      final Long taxId,
      final SampleStatus status,
      final Instant release,
      final Instant update,
      final Instant create,
      final Instant submitted,
      final Instant reviewed,
      final Set<Attribute> attributes,
      final Set<Relationship> relationships,
      final Set<ExternalReference> externalReferences,
      final SubmittedViaType submittedVia) {
    return build(
        name,
        accession,
        sraAccession,
        domain,
        webinSubmissionAccountId,
        taxId,
        status,
        release,
        update,
        create,
        submitted,
        reviewed,
        attributes,
        null,
        null,
        relationships,
        externalReferences,
        null,
        null,
        null,
        null,
        submittedVia);
  }

  // Used for deserializtion (JSON -> Java)
  @JsonCreator
  public static Sample build(
      @JsonProperty("name") final String name,
      @JsonProperty("accession") final String accession,
      @JsonProperty("sraAccession") final String sraAccession,
      @JsonProperty("domain") final String domain,
      @JsonProperty("webinSubmissionAccountId") final String webinSubmissionAccountId,
      @JsonProperty("taxId") final Long taxId,
      @JsonProperty("status") final SampleStatus status,
      @JsonProperty("release") @JsonDeserialize(using = CustomInstantDeserializer.class)
          final Instant release,
      @JsonProperty("update") @JsonDeserialize(using = CustomInstantDeserializer.class)
          final Instant update,
      @JsonProperty("create") @JsonDeserialize(using = CustomInstantDeserializer.class)
          final Instant create,
      @JsonProperty("submitted") @JsonDeserialize(using = CustomInstantDeserializer.class)
          final Instant submitted,
      @JsonProperty("reviewed") @JsonDeserialize(using = CustomInstantDeserializer.class)
          final Instant reviewed,
      @JsonProperty("characteristics") @JsonDeserialize(using = CharacteristicDeserializer.class)
          final Collection<Attribute> attributes,
      @JsonProperty("data") final Collection<AbstractData> data,
      @JsonProperty("structuredData") final Collection<StructuredDataTable> structuredData,
      @JsonProperty("relationships") final Collection<Relationship> relationships,
      @JsonProperty("externalReferences") final Collection<ExternalReference> externalReferences,
      @JsonProperty("organization") final Collection<Organization> organizations,
      @JsonProperty("contact") final Collection<Contact> contacts,
      @JsonProperty("publications") final Collection<Publication> publications,
      @JsonProperty("certificates") final Collection<Certificate> certificates,
      @JsonProperty("submittedVia") final SubmittedViaType submittedVia) {

    final Sample sample = new Sample();

    if (accession != null) {
      sample.accession = accession.trim();
    }

    if (sraAccession != null) {
      sample.sraAccession = sraAccession.trim();
    }

    if (name == null) {
      throw new IllegalArgumentException("Sample name must be provided");
    }

    sample.name = name.trim();

    if (domain != null) {
      sample.domain = domain.trim();
    }

    if (webinSubmissionAccountId != null) {
      sample.webinSubmissionAccountId = webinSubmissionAccountId.trim();
    }

    if (taxId != null) {
      sample.taxId = taxId;
    }

    // Instead of validation  failure, if null, set it to now
    sample.update = update == null ? Instant.now() : update;

    sample.create = create == null ? sample.update : create;

    sample.submitted = submitted;

    sample.reviewed = reviewed;

    // Validation moved to a later stage, to capture the error (SampleService.store())
    sample.release = release;

    if (status != null) {
      sample.status = status;
    } else {
      if (sample.release != null) {
        sample.status =
            sample.release.isAfter(Instant.now()) ? SampleStatus.PRIVATE : SampleStatus.PUBLIC;
      } else {
        sample.status = SampleStatus.PRIVATE;
      }
    }

    sample.attributes = new TreeSet<>();

    if (attributes != null) {
      sample.attributes.addAll(attributes);
    }

    sample.relationships = new TreeSet<>();

    if (relationships != null) {
      sample.relationships.addAll(relationships);
    }

    sample.externalReferences = new TreeSet<>();

    if (externalReferences != null) {
      sample.externalReferences.addAll(externalReferences);
    }

    sample.organizations = new TreeSet<>();

    if (organizations != null) {
      sample.organizations.addAll(organizations);
    }

    sample.contacts = new TreeSet<>();

    if (contacts != null) {
      sample.contacts.addAll(contacts);
    }

    sample.publications = new TreeSet<>();

    if (publications != null) {
      sample.publications.addAll(publications);
    }

    sample.certificates = new TreeSet<>();

    if (certificates != null) {
      sample.certificates.addAll(certificates);
    }

    sample.data = new TreeSet<>();

    if (data != null) {
      sample.data.addAll(data);
    }

    sample.structuredData = new HashSet<>();

    if (structuredData != null) {
      sample.structuredData.addAll(structuredData);
    }

    sample.submittedVia = submittedVia != null ? submittedVia : SubmittedViaType.JSON_API;

    return sample;
  }

  public static class Builder {
    protected String name;
    protected String accession = null;
    protected String sraAccession = null;
    protected String domain = null;
    protected String webinSubmissionAccountId = null;
    protected Long taxId = null;
    protected SampleStatus status = null;
    protected Instant release = Instant.now();
    protected Instant update = Instant.now();
    protected Instant create = Instant.now();
    protected Instant submitted = Instant.now();
    protected Instant reviewed;
    SubmittedViaType submittedVia;
    protected SortedSet<Attribute> attributes = new TreeSet<>();
    protected SortedSet<Relationship> relationships = new TreeSet<>();
    protected SortedSet<ExternalReference> externalReferences = new TreeSet<>();
    protected SortedSet<Organization> organizations = new TreeSet<>();
    protected SortedSet<Contact> contacts = new TreeSet<>();
    protected SortedSet<Publication> publications = new TreeSet<>();
    protected SortedSet<Certificate> certificates = new TreeSet<>();
    protected Set<AbstractData> data = new TreeSet<>();
    protected Set<StructuredDataTable> structuredData = new HashSet<>();

    public Builder(final String name, final String accession) {
      this.name = name;
      this.accession = accession;
    }

    public Builder(final String name, final String accession, final String sraAccession) {
      this.name = name;
      this.accession = accession;
      this.sraAccession = sraAccession;
    }

    public Builder(
        final String name,
        final String accession,
        final String sraAccession,
        final SampleStatus status) {
      this.name = name;
      this.accession = accession;
      this.sraAccession = sraAccession;
      this.status = status;
    }

    public Builder(final String name, final String accession, final SampleStatus status) {
      this.name = name;
      this.accession = accession;
      this.status = status;
    }

    public Builder(final String name) {
      this.name = name;
    }

    public Builder(final String name, final SampleStatus status) {
      this.name = name;
      this.status = status;
    }

    public Builder withAccession(final String accession) {
      this.accession = accession;
      return this;
    }

    public Builder withSraAccession(final String sraAccession) {
      this.sraAccession = sraAccession;
      return this;
    }

    public Builder withDomain(final String domain) {
      this.domain = domain;
      return this;
    }

    public Builder withWebinSubmissionAccountId(final String webinSubmissionAccountId) {
      this.webinSubmissionAccountId = webinSubmissionAccountId;
      return this;
    }

    public Builder withTaxId(final Long taxId) {
      this.taxId = taxId;
      return this;
    }

    public Builder withStatus(final SampleStatus status) {
      this.status = status;
      return this;
    }

    public Builder withRelease(final String release) {
      this.release = Objects.requireNonNull(parseDateTime(release)).toInstant();
      return this;
    }

    public Builder withRelease(final Instant release) {
      this.release = release;
      return this;
    }

    public Builder withUpdate(final Instant update) {
      this.update = update;
      return this;
    }

    public Builder withUpdate(final String update) {
      this.update = Objects.requireNonNull(parseDateTime(update)).toInstant();
      return this;
    }

    public Builder withCreate(final Instant create) {
      this.create = create;
      return this;
    }

    public Builder withCreate(final String create) {
      this.create = Objects.requireNonNull(parseDateTime(create)).toInstant();
      return this;
    }

    public Builder withSubmitted(final Instant submitted) {
      this.submitted = submitted;
      return this;
    }

    public Builder withSubmitted(final String submitted) {
      this.submitted = Objects.requireNonNull(parseDateTime(submitted)).toInstant();
      return this;
    }

    public Builder withNoSubmitted() {
      submitted = null;
      return this;
    }

    public Builder withReviewed(final Instant reviewed) {
      this.reviewed = reviewed;
      return this;
    }

    public Builder withReviewed(final String reviewed) {
      this.reviewed = Objects.requireNonNull(parseDateTime(reviewed)).toInstant();
      return this;
    }

    public Builder withNoReviewed() {
      reviewed = null;
      return this;
    }

    public Builder withSubmittedVia(final SubmittedViaType submittedVia) {
      this.submittedVia = submittedVia;
      return this;
    }

    /** Replace builder's attributes with the provided attribute collection */
    public Builder withAttributes(final Collection<Attribute> attributes) {
      this.attributes = new TreeSet<>(attributes);
      return this;
    }

    public Builder addAttribute(final Attribute attribute) {
      attributes.add(attribute);
      return this;
    }

    public Builder addAllAttributes(final Collection<Attribute> attributes) {
      this.attributes.addAll(attributes);
      return this;
    }

    /** Replace builder structuredData with the provided structuredData collection */
    public Builder withData(final Collection<AbstractData> data) {
      if (data != null) {
        this.data = new TreeSet<>(data);
      } else {
        this.data = new TreeSet<>();
      }

      return this;
    }

    public Builder withStructuredData(final Set<StructuredDataTable> structuredData) {
      this.structuredData = structuredData != null ? structuredData : new HashSet<>();

      return this;
    }

    public Builder addData(final AbstractData data) {
      this.data.add(data);
      return this;
    }

    public Builder addAllData(final Collection<AbstractData> data) {
      this.data.addAll(data);
      return this;
    }

    /** Replace builder's relationships with the provided relationships collection */
    public Builder withRelationships(final Collection<Relationship> relationships) {
      if (relationships != null) {
        this.relationships = new TreeSet<>(relationships);
      }
      return this;
    }

    public Builder addRelationship(final Relationship relationship) {
      relationships.add(relationship);
      return this;
    }

    public Builder addAllRelationships(final Collection<Relationship> relationships) {
      this.relationships.addAll(relationships);
      return this;
    }

    /** Replace builder's externalReferences with the provided external references collection */
    public Builder withExternalReferences(final Collection<ExternalReference> externalReferences) {
      if (externalReferences != null && !externalReferences.isEmpty()) {
        this.externalReferences = new TreeSet<>(externalReferences);
      }
      return this;
    }

    public Builder addExternalReference(final ExternalReference externalReference) {
      externalReferences.add(externalReference);
      return this;
    }

    public Builder addAllExternalReferences(
        final Collection<ExternalReference> externalReferences) {
      this.externalReferences.addAll(externalReferences);
      return this;
    }

    /** Replace builder's organisations with the provided organisation collection */
    public Builder withOrganizations(final Collection<Organization> organizations) {
      if (organizations != null && !organizations.isEmpty()) {
        this.organizations = new TreeSet<>(organizations);
      }
      return this;
    }

    public Builder addOrganization(final Organization organization) {
      organizations.add(organization);
      return this;
    }

    public Builder allAllOrganizations(final Collection<Organization> organizations) {
      this.organizations.addAll(organizations);
      return this;
    }

    /** Replace builder's contacts with the provided contact collection */
    public Builder withContacts(final Collection<Contact> contacts) {
      if (contacts != null && !contacts.isEmpty()) {
        this.contacts = new TreeSet<>(contacts);
      }
      return this;
    }

    public Builder addContact(final Contact contact) {
      contacts.add(contact);
      return this;
    }

    public Builder addAllContacts(final Collection<Contact> contacts) {
      this.contacts.addAll(contacts);
      return this;
    }

    /** Replace the publications with the provided collections */
    public Builder withPublications(final Collection<Publication> publications) {
      if (publications != null && !publications.isEmpty()) {
        this.publications = new TreeSet<>(publications);
      }
      return this;
    }

    /** Add a publication to the list of builder publications */
    public Builder addPublication(final Publication publication) {
      publications.add(publication);
      return this;
    }

    /** Add all publications in the provided collection to the builder publications */
    public Builder addAllPublications(final Collection<Publication> publications) {
      this.publications.addAll(publications);
      return this;
    }

    public Builder withCertificates(final Collection<Certificate> certificates) {
      if (certificates != null && !certificates.isEmpty()) {
        this.certificates = new TreeSet<>(certificates);
      }
      return this;
    }

    public Builder addAllCertificates(final Collection<Certificate> certificates) {
      this.certificates.addAll(certificates);
      return this;
    }

    // Clean accession field
    public Builder withNoAccession() {
      accession = null;
      return this;
    }

    // Clean domain field
    public Builder withNoDomain() {
      domain = null;
      return this;
    }

    // Clean webin account id field
    public Builder withNoWebinSubmissionAccountId() {
      webinSubmissionAccountId = null;
      return this;
    }

    // Clean collection fields
    public Builder withNoAttributes() {
      attributes = new TreeSet<>();
      return this;
    }

    public Builder withNoRelationships() {
      relationships = new TreeSet<>();
      return this;
    }

    public Builder withNoData() {
      data = new TreeSet<>();
      return this;
    }

    public Builder withNoStructuredData() {
      structuredData = new HashSet<>();
      return this;
    }

    public Builder withNoExternalReferences() {
      externalReferences = new TreeSet<>();
      return this;
    }

    public Builder withNoContacts() {
      contacts = new TreeSet<>();
      return this;
    }

    public Builder withNoOrganisations() {
      organizations = new TreeSet<>();
      return this;
    }

    public Builder withNoPublications() {
      publications = new TreeSet<>();
      return this;
    }

    public Sample build() {
      return Sample.build(
          name,
          accession,
          sraAccession,
          domain,
          webinSubmissionAccountId,
          taxId,
          status,
          release,
          update,
          create,
          submitted,
          reviewed,
          attributes,
          data,
          structuredData,
          relationships,
          externalReferences,
          organizations,
          contacts,
          publications,
          certificates,
          submittedVia);
    }

    private ZonedDateTime parseDateTime(final String datetimeString) {
      if (datetimeString.isEmpty()) {
        return null;
      }
      final TemporalAccessor temporalAccessor =
          getFormatter()
              .parseBest(datetimeString, ZonedDateTime::from, LocalDateTime::from, LocalDate::from);
      if (temporalAccessor instanceof ZonedDateTime) {
        return (ZonedDateTime) temporalAccessor;
      } else if (temporalAccessor instanceof LocalDateTime) {
        return ((LocalDateTime) temporalAccessor).atZone(ZoneId.of("UTC"));
      } else {
        return ((LocalDate) temporalAccessor).atStartOfDay(ZoneId.of("UTC"));
      }
    }

    /**
     * Return a Builder produced extracting informations from the sample
     *
     * @param sample the sample to use as reference
     * @return the Builder
     */
    public static Builder fromSample(final Sample sample) {
      return new Builder(sample.getName(), sample.getAccession(), sample.getSraAccession())
          .withDomain(sample.getDomain())
          .withWebinSubmissionAccountId(sample.getWebinSubmissionAccountId())
          .withTaxId(sample.getTaxId())
          .withStatus(sample.getStatus())
          .withRelease(sample.getRelease())
          .withUpdate(sample.getUpdate())
          .withCreate(sample.getCreate())
          .withSubmitted(sample.getSubmitted())
          .withReviewed(sample.getReviewed())
          .withAttributes(sample.getAttributes())
          .withData(sample.getData())
          .withStructuredData(sample.getStructuredData())
          .withRelationships(sample.getRelationships())
          .withExternalReferences(sample.getExternalReferences())
          .withOrganizations(sample.getOrganizations())
          .withPublications(sample.getPublications())
          .withCertificates(sample.getCertificates())
          .withContacts(sample.getContacts())
          .withSubmittedVia(sample.getSubmittedVia());
    }

    private DateTimeFormatter getFormatter() {
      return new DateTimeFormatterBuilder()
          .parseCaseInsensitive()
          .append(ISO_LOCAL_DATE)
          .optionalStart() // time made optional
          .appendLiteral('T')
          .append(ISO_LOCAL_TIME)
          .optionalStart() // zone and offset made optional
          .appendOffsetId()
          .optionalStart()
          .appendLiteral('[')
          .parseCaseSensitive()
          .appendZoneRegionId()
          .appendLiteral(']')
          .optionalEnd()
          .optionalEnd()
          .optionalEnd()
          .toFormatter();
    }
  }
}
