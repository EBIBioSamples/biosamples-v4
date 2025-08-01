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
package uk.ac.ebi.biosamples.jsonld.model;

import com.fasterxml.jackson.annotation.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder(
    value = {
      "@context",
      "@type",
      "@id",
      "identifier",
      "dateCreated",
      "dateModified",
      "dateReleased",
      "dateSubmitted",
      "mainEntity",
      "isPartOf"
    })
public class JsonLDDataRecord implements BioschemasObject {

  private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

  @JsonProperty("@id")
  private String id;

  @JsonProperty("@context")
  private final BioSchemasContext context = new BioSchemasContext();

  @JsonProperty("@type")
  private final String type = "DataRecord";

  @JsonProperty("identifier")
  private String identifier;

  @JsonProperty("mainEntity")
  private JsonLDSample mainEntity;

  private ZonedDateTime dateCreated;

  private ZonedDateTime dateReleased;

  private ZonedDateTime dateSubmitted;

  @JsonProperty("dateModified")
  private ZonedDateTime dateModified;

  private final Map partOf = getDatasetPartOf();

  public String getId() {
    return id;
  }

  public BioSchemasContext getContext() {
    return context;
  }

  public String getIdentifier() {
    return identifier;
  }

  public JsonLDDataRecord identifier(final String identifier) {
    this.identifier = identifier;
    id = identifier;
    return this;
  }

  public JsonLDSample getMainEntity() {
    return mainEntity;
  }

  public JsonLDDataRecord mainEntity(final JsonLDSample mainEntity) {
    this.mainEntity = mainEntity;
    return this;
  }

  //    public ZonedDateTime getDateModified() {
  //        return dateModified;
  //    }

  //    public ZonedDateTime getDateCreated() {
  //        return dateCreated;
  //    }

  @JsonGetter("dateModified")
  public String getDateModified() {
    return dateTimeFormatter.format(dateModified);
  }

  @JsonGetter("dateCreated")
  public String getDateCreated() {
    return dateTimeFormatter.format(dateCreated);
  }

  @JsonGetter("dateSubmitted")
  public String getDateSubmitted() {
    return dateSubmitted != null ? dateTimeFormatter.format(dateSubmitted) : null;
  }

  @JsonGetter("dateReleased")
  public String getDateReleased() {
    return dateTimeFormatter.format(dateReleased);
  }

  @JsonSetter("dateCreated")
  public JsonLDDataRecord dateCreated(final String dateCreated) {
    this.dateCreated =
        LocalDateTime.parse(dateCreated, dateTimeFormatter).atZone(ZoneId.systemDefault());
    return this;
  }

  @JsonSetter("dateModified")
  public JsonLDDataRecord dateModified(final String dateModified) {
    this.dateModified =
        LocalDateTime.parse(dateModified, dateTimeFormatter).atZone(ZoneId.systemDefault());
    return this;
  }

  @JsonSetter("dateSubmitted")
  public JsonLDDataRecord dateSubmitted(final String dateSubmitted) {
    this.dateSubmitted =
        LocalDateTime.parse(dateSubmitted, dateTimeFormatter).atZone(ZoneId.systemDefault());
    return this;
  }

  @JsonSetter("dateReleased")
  public JsonLDDataRecord dateReleased(final String dateReleased) {
    this.dateReleased =
        LocalDateTime.parse(dateReleased, dateTimeFormatter).atZone(ZoneId.systemDefault());
    return this;
  }

  public JsonLDDataRecord dateModified(final ZonedDateTime dateModified) {
    this.dateModified = dateModified;
    return this;
  }

  public JsonLDDataRecord dateCreated(final ZonedDateTime dateCreated) {
    this.dateCreated = dateCreated;
    return this;
  }

  public JsonLDDataRecord dateSubmitted(final ZonedDateTime dateSubmitted) {
    this.dateSubmitted = dateSubmitted;
    return this;
  }

  public JsonLDDataRecord dateReleased(final ZonedDateTime dateReleased) {
    this.dateReleased = dateReleased;
    return this;
  }

  @JsonProperty("isPartOf")
  public Map getDatasetPartOf() {
    final Map<String, String> datasetPartOf = new HashMap<>();
    datasetPartOf.put("@type", "Dataset");
    // TODO Use relative application url and not hard-coded one
    datasetPartOf.put("@id", "https://www.ebi.ac.uk/biosamples/samples");
    return datasetPartOf;
  }
}
