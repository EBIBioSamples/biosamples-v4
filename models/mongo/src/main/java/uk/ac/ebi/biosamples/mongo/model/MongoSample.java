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
import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;
import uk.ac.ebi.biosamples.model.*;
import uk.ac.ebi.biosamples.model.structured.AbstractData;
import uk.ac.ebi.biosamples.service.CustomInstantDeserializer;
import uk.ac.ebi.biosamples.service.CustomInstantSerializer;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@Document
@CompoundIndexes({
  @CompoundIndex(
      name = "sra_accession_index",
      def = "{'attributes.value': 1}",
      partialFilter = "{'attributes.type': 'SRA accession'}",
      background = true)
})
public class MongoSample {
  @Transient public static final String SEQUENCE_NAME = "accession_sequence";
  @Transient public static final String SRA_SEQUENCE_NAME = "sra_accession_sequence";

  @Id protected String accession;

  @Field(targetType = FieldType.STRING, name = "sraAccession")
  @Indexed(background = true)
  private String sraAccession;

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
    if (accession != null && !accession.trim().isEmpty()) {
      return true;
    } else {
      return false;
    }
  }

  @JsonCreator
  public static MongoSample build(
      @JsonProperty("name") final String name,
      @JsonProperty("accession") final String accession,
      @JsonProperty("sraAccession") final String sraAccession,
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
    mongoSample.sraAccession = sraAccession;
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

    if (attributes != null && !attributes.isEmpty()) {
      mongoSample.attributes.addAll(attributes);
    }

    mongoSample.data = new HashSet<>();

    if (structuredData != null && !structuredData.isEmpty()) {
      mongoSample.data.addAll(structuredData);
    }

    mongoSample.relationships = new TreeSet<>();

    if (relationships != null && !relationships.isEmpty()) {
      mongoSample.relationships.addAll(relationships);
    }

    mongoSample.externalReferences = new TreeSet<>();

    if (externalReferences != null && !externalReferences.isEmpty()) {
      mongoSample.externalReferences.addAll(externalReferences);
    }

    mongoSample.organizations = new TreeSet<>();

    if (organizations != null && !organizations.isEmpty()) {
      mongoSample.organizations.addAll(organizations);
    }

    mongoSample.contacts = new TreeSet<>();

    if (contacts != null && !contacts.isEmpty()) {
      mongoSample.contacts.addAll(contacts);
    }

    mongoSample.publications = new TreeSet<>();

    if (publications != null && !publications.isEmpty()) {
      mongoSample.publications.addAll(publications);
    }

    mongoSample.certificates = new TreeSet<>();

    if (certificates != null && !certificates.isEmpty()) {
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
