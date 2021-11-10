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
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.Contact;
import uk.ac.ebi.biosamples.model.Organization;
import uk.ac.ebi.biosamples.model.Publication;
import uk.ac.ebi.biosamples.model.SubmittedViaType;
import uk.ac.ebi.biosamples.model.structured.AbstractData;
import uk.ac.ebi.biosamples.model.structured.StructuredData;
import uk.ac.ebi.biosamples.model.structured.StructuredDataTable;
import uk.ac.ebi.biosamples.service.CustomInstantDeserializer;
import uk.ac.ebi.biosamples.service.CustomInstantSerializer;

import java.time.Instant;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Document
public class MongoStructuredData {
  @Id protected String accession;

  @JsonSerialize(using = CustomInstantSerializer.class)
  @JsonDeserialize(using = CustomInstantDeserializer.class)
  @LastModifiedDate
  @Indexed(background = true)
  protected Instant update;

  @JsonSerialize(using = CustomInstantSerializer.class)
  @JsonDeserialize(using = CustomInstantDeserializer.class)
  protected Instant create;

  protected Set<StructuredDataTable> data;

  public String getAccession() {
    return accession;
  }

  public Instant getUpdate() {
    return update;
  }

  public Instant getCreate() {
    return create;
  }

  public Set<StructuredDataTable> getData() {
    return data;
  }

  public static MongoStructuredData build(String accession, Instant update, Instant create, Set<StructuredDataTable> data) {
    MongoStructuredData mongoStructuredData = new MongoStructuredData();
    mongoStructuredData.accession = accession;
    mongoStructuredData.update = update;
    mongoStructuredData.create = create;
    mongoStructuredData.data = data;
    return mongoStructuredData;
  }

  public static MongoStructuredData build(StructuredData structuredData) {
    MongoStructuredData mongoStructuredData = new MongoStructuredData();
    mongoStructuredData.accession = structuredData.getAccession();
    mongoStructuredData.update = structuredData.getUpdate();
    mongoStructuredData.create = structuredData.getCreate();
    mongoStructuredData.data = structuredData.getData();
    return mongoStructuredData;
  }
}
