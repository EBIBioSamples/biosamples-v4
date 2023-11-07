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
package uk.ac.ebi.biosamples.mongo.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.time.Instant;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import uk.ac.ebi.biosamples.model.*;
import uk.ac.ebi.biosamples.model.structured.AbstractData;
import uk.ac.ebi.biosamples.service.CustomInstantDeserializer;
import uk.ac.ebi.biosamples.service.CustomInstantSerializer;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Document
public class MongoSample {
  @Transient public static final String SEQUENCE_NAME = "accession_sequence";
  @Transient public static final String SRA_SEQUENCE_NAME = "sra_accession_sequence";

  @Id protected String accession;

  @Indexed(background = true)
  private String accessionPrefix;

  @Indexed(background = true)
  private Integer accessionNumber;

  protected String name;

  /** This is the unique permanent ID of the AAP domain/team that owns this sample. */
  @Indexed(background = true)
  protected String domain;

  @Indexed(background = true)
  protected String webinSubmissionAccountId;

  @Indexed(background = true)
  protected Long taxId;

  protected SampleStatus status;

  @JsonSerialize(using = CustomInstantSerializer.class)
  @JsonDeserialize(using = CustomInstantDeserializer.class)
  @Indexed(background = true)
  protected Instant release;

  @JsonSerialize(using = CustomInstantSerializer.class)
  @JsonDeserialize(using = CustomInstantDeserializer.class)
  @LastModifiedDate
  @Indexed(background = true)
  protected Instant update;

  @JsonSerialize(using = CustomInstantSerializer.class)
  @JsonDeserialize(using = CustomInstantDeserializer.class)
  protected Instant create;

  @JsonSerialize(using = CustomInstantSerializer.class)
  @JsonDeserialize(using = CustomInstantDeserializer.class)
  protected Instant submitted;

  @JsonSerialize(using = CustomInstantSerializer.class)
  @JsonDeserialize(using = CustomInstantDeserializer.class)
  protected Instant reviewed;

  protected SortedSet<Attribute> attributes;
  protected SortedSet<MongoRelationship> relationships;
  protected SortedSet<MongoExternalReference> externalReferences;

  private SortedSet<Organization> organizations;
  protected SortedSet<Contact> contacts;
  protected SortedSet<Publication> publications;
  protected SortedSet<MongoCertificate> certificates;

  @Transient protected Set<AbstractData> data;
  private SubmittedViaType submittedVia;

  @JsonIgnore
  public boolean hasAccession() {
    if (accession != null && accession.trim().length() != 0) {
      return true;
    } else {
      return false;
    }
  }

  public String getAccession() {
    return accession;
  }

  public String getAccessionPrefix() {
    return accessionPrefix;
  }

  public Integer getAccessionNumber() {
    return accessionNumber;
  }

  public String getName() {
    return name;
  }

  public String getDomain() {
    return domain;
  }

  public String getWebinSubmissionAccountId() {
    return webinSubmissionAccountId;
  }

  public Long getTaxId() {
    return taxId;
  }

  public SampleStatus getStatus() {
    return status;
  }

  public Instant getRelease() {
    return release;
  }

  public Instant getSubmitted() {
    return submitted;
  }

  public Instant getReviewed() {
    return reviewed;
  }

  public Instant getUpdate() {
    return update;
  }

  public Instant getCreate() {
    return create;
  }

  public SortedSet<Attribute> getAttributes() {
    return attributes;
  }

  public SortedSet<MongoRelationship> getRelationships() {
    return relationships;
  }

  public SortedSet<MongoExternalReference> getExternalReferences() {
    return externalReferences;
  }

  public SortedSet<Organization> getOrganizations() {
    return organizations;
  }

  public SortedSet<Contact> getContacts() {
    return contacts;
  }

  public SortedSet<Publication> getPublications() {
    return publications;
  }

  public SortedSet<MongoCertificate> getCertificates() {
    return certificates;
  }

  public Set<AbstractData> getData() {
    return data;
  }

  public SubmittedViaType getSubmittedVia() {
    return submittedVia;
  }

  @Override
  public boolean equals(final Object o) {

    if (o == this) {
      return true;
    }
    if (!(o instanceof MongoSample)) {
      return false;
    }
    final MongoSample other = (MongoSample) o;
    return Objects.equals(name, other.name)
        && Objects.equals(accession, other.accession)
        && Objects.equals(domain, other.domain)
        && Objects.equals(webinSubmissionAccountId, other.webinSubmissionAccountId)
        && Objects.equals(taxId, other.taxId)
        && Objects.equals(status, other.status)
        && Objects.equals(release, other.release)
        && Objects.equals(update, other.update)
        && Objects.equals(create, other.create)
        && Objects.equals(submitted, other.submitted)
        && Objects.equals(attributes, other.attributes)
        && Objects.equals(relationships, other.relationships)
        && Objects.equals(externalReferences, other.externalReferences)
        && Objects.equals(organizations, other.organizations)
        && Objects.equals(contacts, other.contacts)
        && Objects.equals(publications, other.publications)
        && Objects.equals(submittedVia, other.submittedVia);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        name,
        accession,
        domain,
        webinSubmissionAccountId,
        taxId,
        status,
        release,
        update,
        create,
        submitted,
        attributes,
        relationships,
        externalReferences,
        organizations,
        contacts,
        publications);
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();

    sb.append("MongoSample(");
    sb.append(name);
    sb.append(",");
    sb.append(accession);
    sb.append(",");
    sb.append(domain);
    sb.append(",");
    sb.append(webinSubmissionAccountId);
    sb.append(",");
    sb.append(taxId);
    sb.append(",");
    sb.append(status);
    sb.append(",");
    sb.append(release);
    sb.append(",");
    sb.append(update);
    sb.append(",");
    sb.append(create);
    sb.append(",");
    sb.append(submitted);
    sb.append(",");
    sb.append(reviewed);
    sb.append(",");
    sb.append(attributes);
    sb.append(",");
    sb.append(relationships);
    sb.append(",");
    sb.append(externalReferences);
    sb.append(",");
    sb.append(organizations);
    sb.append(",");
    sb.append(contacts);
    sb.append(",");
    sb.append(publications);
    sb.append(",");
    sb.append(data);
    sb.append(",");
    sb.append(submittedVia);
    sb.append(")");

    return sb.toString();
  }

  @JsonCreator
  public static MongoSample build(
      @JsonProperty("name") final String name,
      @JsonProperty("accession") final String accession,
      @JsonProperty("domain") final String domain,
      @JsonProperty("webinSubmissionAccountId") final String webinSubmissionAccountId,
      @JsonProperty("taxId") final Long taxId,
      @JsonProperty("status") final SampleStatus status,
      @JsonProperty("release") final Instant release,
      @JsonProperty("update") final Instant update,
      @JsonProperty("create") final Instant create,
      @JsonProperty("submitted") final Instant submitted,
      @JsonProperty("reviewed") final Instant reviewed,
      @JsonProperty("attributes") final Set<Attribute> attributes,
      @JsonProperty("data") final Set<AbstractData> structuredData,
      @JsonProperty("relationships") final Set<MongoRelationship> relationships,
      @JsonProperty("externalReferences")
          final SortedSet<MongoExternalReference> externalReferences,
      @JsonProperty("organizations") final SortedSet<Organization> organizations,
      @JsonProperty("contacts") final SortedSet<Contact> contacts,
      @JsonProperty("publications") final SortedSet<Publication> publications,
      @JsonProperty("certificates") final SortedSet<MongoCertificate> certificates,
      @JsonProperty("submittedVia") final SubmittedViaType submittedVia) {

    final MongoSample mongoSample = new MongoSample();

    mongoSample.accession = accession;
    mongoSample.name = name;
    mongoSample.domain = domain;
    mongoSample.webinSubmissionAccountId = webinSubmissionAccountId;
    mongoSample.taxId = taxId;
    mongoSample.status = status;
    mongoSample.release = release;
    mongoSample.update = update;
    mongoSample.create = create;
    mongoSample.submitted = submitted;
    mongoSample.reviewed = reviewed;
    mongoSample.attributes = new TreeSet<>();

    if (attributes != null && attributes.size() > 0) {
      mongoSample.attributes.addAll(attributes);
    }

    mongoSample.data = new HashSet<>();

    if (structuredData != null && structuredData.size() > 0) {
      mongoSample.data.addAll(structuredData);
    }

    mongoSample.relationships = new TreeSet<>();

    if (relationships != null && relationships.size() > 0) {
      mongoSample.relationships.addAll(relationships);
    }

    mongoSample.externalReferences = new TreeSet<>();

    if (externalReferences != null && externalReferences.size() > 0) {
      mongoSample.externalReferences.addAll(externalReferences);
    }

    mongoSample.organizations = new TreeSet<>();

    if (organizations != null && organizations.size() > 0) {
      mongoSample.organizations.addAll(organizations);
    }

    mongoSample.contacts = new TreeSet<>();

    if (contacts != null && contacts.size() > 0) {
      mongoSample.contacts.addAll(contacts);
    }

    mongoSample.publications = new TreeSet<>();

    if (publications != null && publications.size() > 0) {
      mongoSample.publications.addAll(publications);
    }

    mongoSample.certificates = new TreeSet<>();

    if (certificates != null && certificates.size() > 0) {
      mongoSample.certificates.addAll(certificates);
    }

    mongoSample.submittedVia = submittedVia;

    // split accession into prefix & number, if possible
    final Pattern r = Pattern.compile("^(\\D+)(\\d+)$");

    if (accession != null) {
      final Matcher m = r.matcher(accession);

      if (m.matches() && m.groupCount() == 2) {
        mongoSample.accessionPrefix = m.group(1);
        mongoSample.accessionNumber = Integer.parseInt(m.group(2));
      }
    }

    return mongoSample;
  }
}
